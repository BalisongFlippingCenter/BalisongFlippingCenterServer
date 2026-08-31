package com.example.BalisongFlipping.services;

import com.example.BalisongFlipping.dtos.catalogDtos.*;
import com.example.BalisongFlipping.modals.knifeCatalog.*;
import com.example.BalisongFlipping.repositories.KnifeRepository;
import com.example.BalisongFlipping.repositories.MakerRepository;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class KnifeCatalogService {

    private final KnifeRepository knifeRepository;
    private final MakerRepository makerRepository;

    public KnifeCatalogService(KnifeRepository knifeRepository, MakerRepository makerRepository) {
        this.knifeRepository = knifeRepository;
        this.makerRepository = makerRepository;
    }

    public List<KnifeSummaryDto> searchKnives(String search) {
        List<Knife> knives = (search == null || search.isBlank())
                ? knifeRepository.findAll()
                : knifeRepository.findByNameContainingIgnoreCaseOrMakerNameContainingIgnoreCase(search, search);
        return knives.stream().map(this::toSummary).collect(Collectors.toList());
    }

    public KnifeDetailDto getKnifeBySlug(String slug) {
        Knife knife = knifeRepository.findBySlug(slug)
                .orElseThrow(() -> new IllegalArgumentException("No knife found with slug '" + slug + "'"));
        return toDetail(knife);
    }

    public MakerDetailDto getMakerBySlug(String slug) {
        Maker maker = makerRepository.findBySlug(slug)
                .orElseThrow(() -> new IllegalArgumentException("No maker found with slug '" + slug + "'"));
        List<KnifeSummaryDto> knives = knifeRepository.findByMaker(maker).stream()
                .map(this::toSummary)
                .collect(Collectors.toList());
        return new MakerDetailDto(maker.getSlug(), maker.getName(), maker.getCountry(), maker.getKnownFor(), maker.getOfficialSiteUrl(), knives);
    }

    private KnifeSummaryDto toSummary(Knife knife) {
        return new KnifeSummaryDto(
                knife.getSlug(),
                knife.getName(),
                knife.getMaker().getName(),
                knife.getMaker().getSlug(),
                bladeStyleSummary(knife),
                priceRangeSummary(knife),
                hasActiveVersion(knife)
        );
    }

    private KnifeDetailDto toDetail(Knife knife) {
        List<KnifeVersionResponseDto> versions = knife.getVersions().stream()
                .sorted(Comparator.comparing(KnifeVersion::getReleaseYear, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toVersionDto)
                .collect(Collectors.toList());
        return new KnifeDetailDto(
                knife.getSlug(),
                knife.getName(),
                knife.getMaker().getName(),
                knife.getMaker().getSlug(),
                bladeStyleSummary(knife),
                priceRangeSummary(knife),
                versions
        );
    }

    private KnifeVersionResponseDto toVersionDto(KnifeVersion v) {
        List<KnifeVariantResponseDto> variants = v.getVariants().stream()
                .map(variant -> new KnifeVariantResponseDto(
                        variant.getVariantSlug(),
                        enumName(variant.getType()),
                        variant.getLabel(),
                        variant.getMsrp(),
                        enumName(variant.getBladeStyle()),
                        enumName(variant.getBladeMaterial()),
                        enumName(variant.getBladeFinish())
                ))
                .collect(Collectors.toList());

        List<WhereToFindResponseDto> whereToFind = v.getWhereToFind().stream()
                .sorted(Comparator.comparingInt(WhereToFind::getSortOrder))
                .map(w -> new WhereToFindResponseDto(w.getLabel(), w.getUrl(), enumName(w.getType()), w.getNote()))
                .collect(Collectors.toList());

        return new KnifeVersionResponseDto(
                v.getVersionSlug(),
                v.getVersionLabel(),
                v.isDiscontinued(),
                v.getReleaseYear(),
                v.getDescription(),
                v.getOverallLength(),
                v.getWeight(),
                enumName(v.getPivotSystem()),
                enumName(v.getLatchType()),
                enumName(v.getPinSystem()),
                v.isHasModularBalance(),
                v.getBalanceValue(),
                enumName(v.getHandleConstruction()),
                enumName(v.getHandleMaterial()),
                enumName(v.getHandleFinish()),
                variants,
                whereToFind
        );
    }

    private boolean hasActiveVersion(Knife knife) {
        return knife.getVersions().stream().anyMatch(v -> !v.isDiscontinued());
    }

    private String bladeStyleSummary(Knife knife) {
        List<String> styles = knife.getVersions().stream()
                .flatMap(v -> v.getVariants().stream())
                .map(KnifeVariant::getBladeStyle)
                .filter(Objects::nonNull)
                .map(this::humanize)
                .distinct()
                .collect(Collectors.toList());
        return String.join(" / ", styles);
    }

    private String priceRangeSummary(Knife knife) {
        List<Double> prices = knife.getVersions().stream()
                .flatMap(v -> v.getVariants().stream())
                .map(KnifeVariant::getMsrp)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (prices.isEmpty()) return null;
        double min = Collections.min(prices);
        double max = Collections.max(prices);
        if (min == max) return String.format("$%.0f", min);
        return String.format("$%.0f–$%.0f", min, max);
    }

    private String enumName(Enum<?> value) {
        return value == null ? null : value.name();
    }

    private String humanize(Enum<?> value) {
        String[] words = value.name().split("_");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(w.substring(0, 1)).append(w.substring(1).toLowerCase());
        }
        return sb.toString();
    }
}
