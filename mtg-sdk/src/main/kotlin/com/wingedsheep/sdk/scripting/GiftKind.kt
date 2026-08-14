package com.wingedsheep.sdk.scripting

import kotlinx.serialization.Serializable

/**
 * The `[something]` in "Gift a [something]" (CR 702.174d–i) — what the opponent chosen as the
 * gift cost receives.
 *
 * The rules enumerate the whole list, and each entry maps to exactly one effect, so a gift card
 * only names its kind: [com.wingedsheep.sdk.dsl.giftEffect] derives the effect and
 * [com.wingedsheep.sdk.dsl.giftEnterTrigger] derives the permanent's gift triggered ability
 * (CR 702.174b) from it.
 *
 * Pairs with [KeywordAbility.Gift] and the `gift(kind)` DSL helper on
 * [com.wingedsheep.sdk.dsl.CardBuilder].
 */
@Serializable
enum class GiftKind(
    /** Reminder-text noun phrase — renders as "Gift <label>", e.g. "Gift a tapped Fish". */
    val label: String,
    /** What the chosen player gets, phrased for the derived ability's rules text. */
    val effectText: String
) {
    /** CR 702.174e — "The chosen player draws a card." */
    CARD("a card", "the chosen player draws a card"),

    /** CR 702.174d — "The chosen player creates a Food token." */
    FOOD("a Food", "the chosen player creates a Food token"),

    /** CR 702.174f — "The chosen player creates a tapped 1/1 blue Fish creature token." */
    TAPPED_FISH("a tapped Fish", "the chosen player creates a tapped 1/1 blue Fish creature token"),

    /** CR 702.174h — "The chosen player creates a Treasure token." */
    TREASURE("a Treasure", "the chosen player creates a Treasure token"),

    /** CR 702.174i — "The chosen player creates an 8/8 blue Octopus creature token." */
    OCTOPUS("an Octopus", "the chosen player creates an 8/8 blue Octopus creature token"),

    /** CR 702.174g — "The chosen player takes an extra turn after this one." */
    EXTRA_TURN("an extra turn", "the chosen player takes an extra turn after this one"),
}
