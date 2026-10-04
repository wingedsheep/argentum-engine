package com.wingedsheep.mtg.sets.definitions.c21.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Laelia, the Blade Reforged — Commander 2021 #53 (reprinted in Commander Legends: Battle for
 * Baldur's Gate and Modern Horizons 3)
 * {2}{R} · Legendary Creature — Spirit Warrior · 2/2
 *
 * Haste
 * Whenever Laelia attacks, exile the top card of your library. You may play that card this turn.
 * Whenever one or more cards are put into exile from your library and/or your graveyard, put a
 * +1/+1 counter on Laelia.
 *
 * - The attack trigger is the shared gather → exile → grant impulse pipeline (Grotag Night-Runner).
 * - The counter trigger is the exile batch (`CardsPutIntoExileEvent`, CR 603.2c: once per batch)
 *   watching LIBRARY + GRAVEYARD. `youControl()` narrows it to *your* library and graveyard — for a
 *   non-battlefield zone the detector reads the controller predicate as ownership. Who does the
 *   exiling is irrelevant (ruling 2024-06-07), which the batch already ignores.
 */
val LaeliaTheBladeReforged = card("Laelia, the Blade Reforged") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Spirit Warrior"
    power = 2
    toughness = 2
    oracleText = "Haste\nWhenever Laelia attacks, exile the top card of your library. You may play that card this turn.\n" +
        "Whenever one or more cards are put into exile from your library and/or your graveyard, put a +1/+1 counter on Laelia."

    keywords(Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.Pipeline {
            val exiledCard = gather(CardSource.TopOfLibrary(1))
            exile(exiledCard)
            run(Effects.GrantMayPlayFromExile(exiledCard))
        }
    }

    triggeredAbility {
        trigger = Triggers.oneOrMore(GameObjectFilter.Any.youControl())
            .putIntoExile(setOf(Zone.LIBRARY, Zone.GRAVEYARD))
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "53"
        artist = "Wisnu Tan"
        imageUri = "https://cards.scryfall.io/normal/front/a/3/a3bb2881-e8fb-4fba-a9f9-d93e6ca24378.jpg?1783927592"
        ruling("2024-06-07", "The last ability triggers only once for each time cards are put into exile from your library and/or graveyard, no matter how many cards were exiled at the same time.")
        ruling("2024-06-07", "Laelia's last triggered ability doesn't care which player is exiling cards from the library or graveyard. Cards put into exile from your library or graveyard for any reason, such as the delve ability, cause the ability to trigger.")
        ruling("2024-06-07", "If you play a card this way, it leaves exile and becomes a new object. If it returns to exile later in the turn, you can't play it again.")
    }
}
