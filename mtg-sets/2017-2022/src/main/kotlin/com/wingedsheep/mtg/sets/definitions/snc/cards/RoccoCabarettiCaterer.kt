package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Rocco, Cabaretti Caterer — Streets of New Capenna #218
 * {X}{R}{G}{W} · Legendary Creature — Elf Druid · 3/1
 *
 * When Rocco enters, if you cast it, you may search your library for a creature card with mana
 * value X or less, put it onto the battlefield, then shuffle.
 *
 * The trigger resolves after Rocco has left the stack, so the cap reads the X paid for it off the
 * permanent's cast record ([DynamicAmounts.castX], as Progenitor Exarch's ETB does) rather than
 * the spell's own X. A free cast reads X = 0, per the ruling.
 */
val RoccoCabarettiCaterer = card("Rocco, Cabaretti Caterer") {
    manaCost = "{X}{R}{G}{W}"
    colorIdentity = "RGW"
    typeLine = "Legendary Creature — Elf Druid"
    power = 3
    toughness = 1
    oracleText = "When Rocco enters, if you cast it, you may search your library for a creature card " +
        "with mana value X or less, put it onto the battlefield, then shuffle."

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.WasCast
        effect = Effects.May(
            Patterns.Library.searchLibrary(
                filter = GameObjectFilter.Creature.manaValueAtMostDynamic(DynamicAmounts.castX()),
                count = 1,
                destination = SearchDestination.BATTLEFIELD,
                shuffleAfter = true
            )
        )
        description = "When Rocco enters, if you cast it, you may search your library for a creature " +
            "card with mana value X or less, put it onto the battlefield, then shuffle."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "218"
        artist = "Chris Seaman"
        flavorText = "They wouldn't admit it, but some people join the Cabaretti just for the food."
        imageUri = "https://cards.scryfall.io/normal/front/b/6/b6cf8b35-2a81-40fd-b383-becb81bef806.jpg?1783923072"
        ruling("2022-04-29", "If an effect allows you to cast Rocco without paying its mana cost, X is 0.")
    }
}
