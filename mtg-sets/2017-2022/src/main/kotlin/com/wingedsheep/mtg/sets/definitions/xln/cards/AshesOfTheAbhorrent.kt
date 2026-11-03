package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PlayersCantActivateAbilities
import com.wingedsheep.sdk.scripting.PlayersCantCastSpells
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Ashes of the Abhorrent
 * {1}{W}
 * Enchantment
 * Players can't cast spells from graveyards or activate abilities of cards in graveyards.
 * Whenever a creature dies, you gain 1 life.
 *
 * The cast half is the graveyard "where" axis of Kunoros's lock. The activation half is
 * `PlayersCantActivateAbilities` widened past permanents (`anyZone = true`) and narrowed to sources
 * sitting in a graveyard right now (`currentlyIn(GRAVEYARD)`), so battlefield, hand and exile
 * abilities are untouched. Triggered abilities of graveyard cards and playing lands from a
 * graveyard are unaffected (the rulings).
 */
val AshesOfTheAbhorrent = card("Ashes of the Abhorrent") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "Players can't cast spells from graveyards or activate abilities of cards in graveyards.\n" +
        "Whenever a creature dies, you gain 1 life."

    staticAbility {
        ability = PlayersCantCastSpells(Player.Each, fromZones = setOf(Zone.GRAVEYARD))
    }
    staticAbility {
        ability = PlayersCantActivateAbilities(
            Player.Each,
            permanentFilter = GameObjectFilter.Any.currentlyIn(Zone.GRAVEYARD),
            anyZone = true
        )
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature).dies()
        effect = Effects.GainLife(1)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "2"
        artist = "Daarken"
        flavorText = "\"Let no trace of the vampires' foulness remain.\""
        imageUri = "https://cards.scryfall.io/normal/front/4/e/4e8eb264-dadb-440c-af85-273e755f1db6.jpg?1783935806"
        ruling(
            "2017-09-29",
            "Activated abilities contain a colon. They're generally written \"[Cost]: [Effect].\" Some keyword " +
                "abilities (such as embalm from the Amonkhet set) are activated abilities and will have colons " +
                "in their reminder text. Triggered abilities (starting with \"when,\" \"whenever,\" or \"at\") " +
                "are unaffected by Ashes of the Abhorrent."
        )
        ruling(
            "2017-09-29",
            "Ashes of the Abhorrent doesn't stop players from playing land cards from the graveyard if an effect " +
                "allows them to do so."
        )
        ruling("2017-09-29", "If a creature dies at the same time that Ashes of the Abhorrent is destroyed, you'll gain 1 life.")
    }
}
