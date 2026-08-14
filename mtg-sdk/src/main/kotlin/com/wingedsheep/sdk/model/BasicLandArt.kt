package com.wingedsheep.sdk.model

/**
 * Ordering over the art variants a set prints of one basic land type.
 *
 * Paper numbers the regular booster arts inside the main set numbering and appends special
 * treatments — full-art, extended, borderless — above the set's card count: Bloomburrow's Plains
 * are 262-265 regular and 369-370 full-art, Lord of the Rings' are 262-263 regular and 272-273
 * full-art map, Final Fantasy's are 294-296 regular and 572 borderless. Ascending collector number
 * therefore puts a set's **standard** art first, which is the single printing limited deck building
 * hands out (`BoosterGenerator.getBasicLands`).
 *
 * Variants with an absent or non-numeric collector number sort last: they hold no position in the
 * set's numbering, so they can't claim to be its standard art.
 */
object BasicLandArt {

    /**
     * Standard art first, then the set's remaining treatments in printed order. Total and
     * deterministic — [com.wingedsheep.sdk.model.MtgSet.basicLands] is ordered by it so every
     * consumer sees the same variant sequence across runs, not whatever order reflection yielded.
     */
    val standardFirst: Comparator<CardDefinition> = compareBy(
        { it.metadata.collectorNumber?.toIntOrNull() ?: Int.MAX_VALUE },
        { it.metadata.collectorNumber ?: "" },
    )
}
