package com.example.BalisongFlipping.seed;

import com.example.BalisongFlipping.enums.knives.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Maps loose, human-authored spec strings (from the source-of-truth JSON, and eventually
 * from scraped/LLM-extracted text) onto the app's canonical spec enums via keyword matching,
 * since source text uses modifiers and spellings ("410 Stainless Steel", "7075 Aluminum")
 * that don't line up with the fixed-vocabulary switch statements used for user-entered
 * collection data.
 */
public class KnifeSpecNormalizer {

    private static final Logger log = LoggerFactory.getLogger(KnifeSpecNormalizer.class);

    private static boolean has(String haystack, String needle) {
        return haystack.contains(needle);
    }

    public static BladeStyle bladeStyle(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String v = raw.toLowerCase().trim();
        if (has(v, "japanese") && has(v, "tanto")) return BladeStyle.JAPANESE_TANTO;
        if (has(v, "american") && has(v, "tanto")) return BladeStyle.AMERICAN_TANTO;
        if (has(v, "tanto")) return BladeStyle.TANTO;
        if (has(v, "bowie")) return BladeStyle.BOWIE;
        if (has(v, "kukri")) return BladeStyle.KUKRI;
        if (has(v, "spear")) return BladeStyle.SPEAR_POINT;
        if (has(v, "weehawk")) return BladeStyle.WEEHAWK;
        if (has(v, "horse")) return BladeStyle.HORSE_SHOE;
        if (has(v, "other")) return BladeStyle.OTHER;
        log.warn("Unrecognized blade style '{}', defaulting to UNKNOWN", raw);
        return BladeStyle.UNKNOWN;
    }

    public static BladeMaterial bladeMaterial(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String v = raw.toLowerCase().trim();
        if (has(v, "stainless")) return BladeMaterial.STAINLESS_STEEL;
        if (has(v, "titanium")) return BladeMaterial.TITANIUM;
        if (has(v, "d2")) return BladeMaterial.D2;
        if (has(v, "s35vn")) return BladeMaterial.S35VN;
        if (has(v, "s32vn")) return BladeMaterial.S32VN;
        if (has(v, "hardened")) return BladeMaterial.HARDENED_STEEL;
        if (has(v, "plastic")) return BladeMaterial.PLASTIC;
        if (has(v, "alumin")) {
            if (has(v, "7075")) return BladeMaterial.ALUMINIUM_7075;
            if (has(v, "6061")) return BladeMaterial.ALUMINIUM_6061;
            return BladeMaterial.ALUMINIUM;
        }
        if (has(v, "other")) return BladeMaterial.OTHER;
        log.warn("Unrecognized blade material '{}', defaulting to UNKNOWN", raw);
        return BladeMaterial.UNKNOWN;
    }

    public static BladeFinish bladeFinish(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String v = raw.toLowerCase().trim();
        if (has(v, "mirror")) return BladeFinish.MIRROR_POLISHED;
        if (has(v, "stone")) return BladeFinish.STONE_WASH;
        if (has(v, "acid")) return BladeFinish.ACID_WASH;
        if (has(v, "black")) return BladeFinish.BLACK_WASH;
        if (has(v, "dual")) return BladeFinish.DUALTONE;
        if (has(v, "dlc")) return BladeFinish.DLC;
        if (has(v, "satin")) return BladeFinish.SATIN;
        if (has(v, "polish")) return BladeFinish.POLISHED;
        if (has(v, "plain")) return BladeFinish.PLAIN;
        if (has(v, "other")) return BladeFinish.OTHER;
        log.warn("Unrecognized blade finish '{}', defaulting to UNKNOWN", raw);
        return BladeFinish.UNKNOWN;
    }

    public static HandleConstruction handleConstruction(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String v = raw.toLowerCase().trim();
        if (has(v, "chanwhich")) return HandleConstruction.CHANWHICH;
        if (has(v, "channel") || has(v, "chanel")) return HandleConstruction.CHANNEL;
        if (has(v, "sandwhich") || has(v, "sandwich")) return HandleConstruction.SANDWHICH;
        if (has(v, "other")) return HandleConstruction.OTHER;
        log.warn("Unrecognized handle construction '{}', defaulting to UNKNOWN", raw);
        return HandleConstruction.UNKNOWN;
    }

    public static HandleMaterial handleMaterial(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String v = raw.toLowerCase().trim();
        boolean g10 = has(v, "g-10") || has(v, "g10") || has(v, "g 10");
        if (g10) {
            if (has(v, "titanium")) return HandleMaterial.G_10_TITANIUM;
            if (has(v, "alumin")) return HandleMaterial.G_10_ALUMINIUM;
            return HandleMaterial.G_10;
        }
        if (has(v, "carbon")) return HandleMaterial.CARBON_FIBER;
        if (has(v, "stainless")) return HandleMaterial.STAINLESS_STEEL;
        if (has(v, "hardened")) return HandleMaterial.HARDENED_STEEL;
        if (has(v, "titanium")) return HandleMaterial.TITANIUM;
        if (has(v, "plastic")) return HandleMaterial.PLASTIC;
        if (has(v, "alumin")) {
            if (has(v, "7075")) return HandleMaterial.ALUMINIUM_7075;
            if (has(v, "6061")) return HandleMaterial.ALUMINIUM_6061;
            return HandleMaterial.ALUMINIUM;
        }
        if (has(v, "other")) return HandleMaterial.OTHER;
        log.warn("Unrecognized handle material '{}', defaulting to UNKNOWN", raw);
        return HandleMaterial.UNKNOWN;
    }

    public static HandleFinish handleFinish(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String v = raw.toLowerCase().trim();
        if (has(v, "anodized")) return HandleFinish.ANODIZED;
        if (has(v, "mirror")) return HandleFinish.MIRROR_POLISHED;
        if (has(v, "bead")) return HandleFinish.BEAD_BLASTED;
        if (has(v, "zir")) return HandleFinish.ZIR_BLASTED;
        if (has(v, "stone")) return HandleFinish.STONE_WASH;
        if (has(v, "acid")) return HandleFinish.ACID_WASHED;
        if (has(v, "black")) return HandleFinish.BLACK_WASH;
        if (has(v, "satin")) return HandleFinish.SATIN;
        if (has(v, "polish")) return HandleFinish.POLISHED;
        if (has(v, "plain")) return HandleFinish.PLAIN;
        if (has(v, "other")) return HandleFinish.OTHER;
        log.warn("Unrecognized handle finish '{}', defaulting to UNKNOWN", raw);
        return HandleFinish.UNKNOWN;
    }

    public static PivotSystem pivotSystem(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String v = raw.toLowerCase().trim();
        if (has(v, "bushing")) return PivotSystem.BUSHINGS;
        if (has(v, "bearing")) return PivotSystem.BEARINGS;
        if (has(v, "washer")) return PivotSystem.WASHERS;
        if (has(v, "other")) return PivotSystem.OTHER;
        log.warn("Unrecognized pivot system '{}', defaulting to UNKNOWN", raw);
        return PivotSystem.UNKNOWN;
    }

    public static LatchType latchType(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String v = raw.toLowerCase().trim();
        if (has(v, "latchless") || (has(v, "no") && has(v, "latch"))) return LatchType.LATCHLESS;
        if (has(v, "spring")) return LatchType.SPRING_LATCH;
        if (has(v, "swing")) return LatchType.SWING_LATCH;
        if (has(v, "other")) return LatchType.OTHER;
        log.warn("Unrecognized latch type '{}', defaulting to UNKNOWN", raw);
        return LatchType.UNKNOWN;
    }

    public static PinSystem pinSystem(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String v = raw.toLowerCase().trim();
        if (has(v, "hidden") && has(v, "zen")) return PinSystem.HIDDEN_ZEN_PINS;
        if (has(v, "zen")) return PinSystem.ZEN_PINS;
        if (has(v, "tang")) return PinSystem.TANG_PINS;
        if (has(v, "pinless") || has(v, "pinsless") || (has(v, "no") && has(v, "pin"))) return PinSystem.PINSLESS;
        if (has(v, "other")) return PinSystem.OTHER;
        log.warn("Unrecognized pin system '{}', defaulting to UNKNOWN", raw);
        return PinSystem.UNKNOWN;
    }

    public static KnifeType variantType(String raw) {
        String v = raw == null ? "" : raw.toLowerCase().trim();
        if (v.equals("trainer")) return KnifeType.TRAINER;
        if (v.equals("live")) return KnifeType.LIVE_BLADE;
        throw new IllegalStateException("Unrecognized variant type '" + raw + "' — expected 'trainer' or 'live'");
    }

    public static SourceType sourceType(String raw) {
        if (raw == null || raw.isBlank()) return SourceType.SECONDARY;
        try {
            return SourceType.valueOf(raw.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            log.warn("Unrecognized source type '{}', defaulting to SECONDARY", raw);
            return SourceType.SECONDARY;
        }
    }

    private KnifeSpecNormalizer() {}
}
