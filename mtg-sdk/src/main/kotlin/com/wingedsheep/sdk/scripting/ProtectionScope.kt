package com.wingedsheep.sdk.scripting

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The quality a Protection or Hexproof keyword ability is from.
 *
 * Mirrors the cases of MTG's "from X" qualifier so [KeywordAbility.Protection] and
 * [KeywordAbility.Hexproof] share a single sealed cost shape (Rule 702.16, 702.11b).
 */
@Serializable
sealed interface ProtectionScope {
    /** Protection / hexproof from a single color. */
    @SerialName("ProtectionScope.Color")
    @Serializable
    data class Color(val color: com.wingedsheep.sdk.core.Color) : ProtectionScope

    /** Protection from multiple colors — "from white and from blue". */
    @SerialName("ProtectionScope.Colors")
    @Serializable
    data class Colors(val colors: Set<com.wingedsheep.sdk.core.Color>) : ProtectionScope

    /**
     * The complement of a single color — "from nongreen". Matches every source that is **not**
     * [color], colorless sources included (CR 105.2c: a colorless object has no color, so it is
     * "nongreen"). A green-white source is green, so it does not match.
     *
     * Engine-wired for *hexproof* (Thrun, Breaker of Silence: "can't be the target of nongreen
     * spells your opponents control or abilities from nongreen sources your opponents control")
     * and for player protection; creature *protection* from a non-color is not projected.
     */
    @SerialName("ProtectionScope.NonColor")
    @Serializable
    data class NonColor(val color: com.wingedsheep.sdk.core.Color) : ProtectionScope

    /** Protection from a card type — "from creatures". */
    @SerialName("ProtectionScope.CardType")
    @Serializable
    data class CardType(val cardType: String) : ProtectionScope

    /** Protection from a creature subtype — "from Goblins". */
    @SerialName("ProtectionScope.Subtype")
    @Serializable
    data class Subtype(val subtype: String) : ProtectionScope

    /** Protection from a supertype — "from legendary creatures" (Tsabo Tavoc). */
    @SerialName("ProtectionScope.Supertype")
    @Serializable
    data class Supertype(val supertype: String) : ProtectionScope

    /** Protection from everything (Rule 702.16i). */
    @SerialName("ProtectionScope.Everything")
    @Serializable
    data object Everything : ProtectionScope

    /** Protection from each of the controller's opponents (Rule 702.16e). */
    @SerialName("ProtectionScope.EachOpponent")
    @Serializable
    data object EachOpponent : ProtectionScope
}
