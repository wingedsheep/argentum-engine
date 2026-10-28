package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Drana, the Last Bloodchief — Zendikar Rising #98
 * {3}{B}{B} · Legendary Creature — Vampire Cleric 4/4
 *
 * Flying
 * Whenever Drana attacks, defending player chooses a nonlegendary creature card in your graveyard.
 * You return that card to the battlefield with an additional +1/+1 counter on it. The creature is a
 * Vampire in addition to its other types.
 *
 * The trigger is untargeted: the defending player picks on resolution (O-Kagachi Made Manifest's
 * shape), and the chosen card enters with the counter in the same move. Only a card that actually
 * arrived gains the Vampire type, and permanently — it stays a Vampire after Drana leaves.
 */
val DranaTheLastBloodchief = card("Drana, the Last Bloodchief") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Vampire Cleric"
    oracleText = "Flying\n" +
        "Whenever Drana attacks, defending player chooses a nonlegendary creature card in your graveyard. " +
        "You return that card to the battlefield with an additional +1/+1 counter on it. The creature is a " +
        "Vampire in addition to its other types."
    power = 4
    toughness = 4

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.Pipeline {
            val graveyard = gather(CardSource.FromZone(Zone.GRAVEYARD, Player.You))
            val chosen = chooseExactly(
                1,
                from = graveyard,
                chooser = Chooser.DefendingPlayer,
                filter = GameObjectFilter.Creature.nonlegendary(),
                prompt = "Choose a nonlegendary creature card for the attacking player to return to the battlefield",
            )
            val returned = moveTracked(
                chosen,
                CardDestination.ToZone(Zone.BATTLEFIELD, Player.You),
                addCounterType = CounterType.PLUS_ONE_PLUS_ONE,
            )
            run(
                Effects.ForEachInCollection(
                    returned,
                    Effects.AddCreatureType("Vampire", EffectTarget.IterationEntity, Duration.Permanent),
                )
            )
        }
        description = "Whenever Drana attacks, defending player chooses a nonlegendary creature card in your " +
            "graveyard. You return that card to the battlefield with an additional +1/+1 counter on it. The " +
            "creature is a Vampire in addition to its other types."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "98"
        artist = "Tyler Jacobson"
        imageUri = "https://cards.scryfall.io/normal/front/5/c/5cd4154e-7681-444b-a0f2-33e887791ba7.jpg?1783929379"
        ruling("2020-09-25", "If Drana attacks a planeswalker, that planeswalker's controller is the defending player.")
        ruling(
            "2020-09-25",
            "The defending player chooses a card and you put it onto the battlefield all while Drana's ability " +
                "is resolving. No player may take actions in between those events."
        )
        ruling(
            "2020-09-25",
            "Because all attackers are declared at the same time, the returned creature can't attack during the " +
                "same combat it returns, even if it has haste."
        )
        ruling(
            "2020-09-25",
            "The returned creature remains a Vampire in addition to its other types even after Drana leaves the " +
                "battlefield."
        )
    }
}
