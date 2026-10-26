package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Sigrid, God-Favored
 * {1}{W}{W}
 * Legendary Creature — Human Warrior
 * 2/2
 * Flash
 * First strike, protection from God creatures
 * When Sigrid enters, exile up to one target attacking or blocking creature until Sigrid leaves
 * the battlefield.
 *
 * "Protection from God creatures" is a subtype narrowed to a card type, so a God that isn't
 * currently a creature (a Theros God below its devotion) doesn't match. The exile is
 * [Effects.MoveUntilSourceLeaves]: the return is a one-shot duration, not a leaves trigger, so it
 * happens even if Sigrid lost her abilities (CR 610.3).
 */
val SigridGodFavored = card("Sigrid, God-Favored") {
    manaCost = "{1}{W}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Human Warrior"
    power = 2
    toughness = 2
    oracleText = "Flash\n" +
        "First strike, protection from God creatures\n" +
        "When Sigrid enters, exile up to one target attacking or blocking creature until Sigrid leaves the battlefield."

    keywords(Keyword.FLASH, Keyword.FIRST_STRIKE)
    keywordAbility(KeywordAbility.protectionFromSubtype("God", "Creature"))

    triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.AttackingOrBlockingCreature, optional = true)
        effect = Effects.MoveUntilSourceLeaves(creature, Zone.EXILE)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "29"
        artist = "Johannes Voss"
        flavorText = "Never alone, never afraid."
        imageUri = "https://cards.scryfall.io/normal/front/a/e/aeec3c1e-e612-4700-888e-300912932552.jpg?1783928275"
    }
}
