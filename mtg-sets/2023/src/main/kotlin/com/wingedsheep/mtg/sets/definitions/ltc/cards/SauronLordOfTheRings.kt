package com.wingedsheep.mtg.sets.definitions.ltc.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Sauron, Lord of the Rings — Tales of Middle-earth Commander #4
 * {5}{U}{B}{R} · Legendary Creature — Avatar Horror · 9/9 · Mythic
 *
 * When you cast this spell, amass Orcs 5, mill five cards, then return a creature card from
 * your graveyard to the battlefield.
 * Trample
 * Whenever a commander an opponent controls dies, the Ring tempts you.
 *
 * The cast trigger resolves while Sauron is still on the stack. The returned creature card is
 * chosen on resolution (no target) and may be one that was already in the graveyard before the
 * mill. The commander designation survives the zone change (CR 903.3), so the dies trigger reads
 * it off the card in the graveyard, before the owner moves it to the command zone.
 */
val SauronLordOfTheRings = card("Sauron, Lord of the Rings") {
    manaCost = "{5}{U}{B}{R}"
    colorIdentity = "UBR"
    typeLine = "Legendary Creature — Avatar Horror"
    power = 9
    toughness = 9
    oracleText = "When you cast this spell, amass Orcs 5, mill five cards, then return a creature card " +
        "from your graveyard to the battlefield.\n" +
        "Trample\n" +
        "Whenever a commander an opponent controls dies, the Ring tempts you."

    // When you cast this spell, amass Orcs 5, mill five cards, then return a creature card from
    // your graveyard to the battlefield.
    triggeredAbility {
        trigger = Triggers.self.isCast()
        effect = Effects.Pipeline {
            run(Effects.Amass(5, "Orc"))
            mill(5)
            val creatures = gather(CardSource.FromZone(Zone.GRAVEYARD, Player.You, GameObjectFilter.Creature))
            val chosen = chooseExactly(
                1,
                from = creatures,
                prompt = "Choose a creature card to return to the battlefield"
            )
            move(chosen, CardDestination.ToZone(Zone.BATTLEFIELD))
        }
        description = "Amass Orcs 5, mill five cards, then return a creature card from your graveyard to the battlefield."
    }

    keywords(Keyword.TRAMPLE)

    // Whenever a commander an opponent controls dies, the Ring tempts you.
    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Any.commander().opponentControls()).dies()
        effect = Effects.TheRingTemptsYou()
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "4"
        artist = "Alex Brock"
        imageUri = "https://cards.scryfall.io/normal/front/f/c/fc53dec0-79fe-4f6f-9d5b-cf298588e808.jpg?1783916038"
        ruling(
            "2023-06-16",
            "The creature card you choose doesn't need to be a card you milled. You may choose a creature card " +
                "that was already in your graveyard before you milled."
        )
        ruling(
            "2023-06-16",
            "Sauron, Lord of the Rings's last ability will trigger even if the owner of the commander that died " +
                "chooses to return it to the command zone after it dies."
        )
    }
}
