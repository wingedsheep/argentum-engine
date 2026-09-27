package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Nature's Will
 * {2}{G}{G}
 * Enchantment
 * Whenever one or more creatures you control deal combat damage to a player, tap all lands that
 * player controls and untap all lands you control.
 *
 * A batch combat-damage trigger: fires once per damaged player per combat-damage step, not once
 * per connecting creature. "That player" is the damaged player (the trigger's triggering player).
 */
val NaturesWill = card("Nature's Will") {
    manaCost = "{2}{G}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment"
    oracleText = "Whenever one or more creatures you control deal combat damage to a player, tap all lands " +
        "that player controls and untap all lands you control."

    triggeredAbility {
        trigger = Triggers.oneOrMore(GameObjectFilter.Creature).dealCombatDamageToAPlayer()
        effect = Patterns.Group.tapAll(GroupFilter(GameObjectFilter.Land.controlledByTriggeringPlayer())) then
            Patterns.Group.untapGroup(GroupFilter(GameObjectFilter.Land.youControl()))
        description = "Whenever one or more creatures you control deal combat damage to a player, tap all " +
            "lands that player controls and untap all lands you control."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "230"
        artist = "Mitch Cotie"
        flavorText = "\"Without the kami to speak to nature on our behalf, we must beg help from nature " +
            "directly.\"\n—Seshiro the Anointed"
        imageUri = "https://cards.scryfall.io/normal/front/7/5/75a291a0-db0d-4ccc-b7ae-240cafa41883.jpg?1783944286"
        ruling(
            "2004-12-01",
            "The lands are tapped and untapped once each time combat damage is dealt (usually once each " +
                "turn, but first strike, double strike, and multiple combat phases can change that), not " +
                "once for each creature that deals combat damage."
        )
    }
}
