package com.wingedsheep.mtg.sets.definitions.fic.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.conditions.Exists
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerExpiry
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Chocobo Camp — Final Fantasy Commander #462
 * Land
 *
 * This land enters tapped unless you control a legendary creature.
 * {T}: Add {G}. When you next cast a Bird creature spell this turn, it enters with an additional
 * +1/+1 counter on it.
 * {2}{G}{G}, {T}: Create a 2/2 green Bird creature token with "Whenever a land you control enters,
 * this token gets +1/+0 until end of turn."
 *
 * The enters-tapped clause is Mines of Moria's `EntersTapped(unlessCondition = Exists(...))`. The
 * mana ability's rider is a one-shot "when you next cast" delayed trigger (Codie, Vociferous Codex's
 * shape) whose body puts the counter on the Bird spell itself, which carries it onto the battlefield
 * (Kumano Faces Kakkazan's chapter II). The token is Sidequest: Raise a Chocobo's landfall Bird.
 */
val ChocoboCamp = card("Chocobo Camp") {
    typeLine = "Land"
    colorIdentity = "G"
    oracleText = "This land enters tapped unless you control a legendary creature.\n" +
        "{T}: Add {G}. When you next cast a Bird creature spell this turn, it enters with an additional +1/+1 counter on it.\n" +
        "{2}{G}{G}, {T}: Create a 2/2 green Bird creature token with \"Whenever a land you control enters, this token gets +1/+0 until end of turn.\""

    replacementEffect(
        EntersTapped(
            unlessCondition = Exists(
                player = Player.You,
                zone = Zone.BATTLEFIELD,
                filter = GameObjectFilter.Creature.legendary()
            )
        )
    )

    // {T}: Add {G}. When you next cast a Bird creature spell this turn, it enters with an
    // additional +1/+1 counter on it.
    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.GREEN) then
            Effects.CreateDelayedTrigger(
                trigger = Triggers.you.casts(GameObjectFilter.Creature.withSubtype("Bird")),
                fireOnce = true,
                expiry = DelayedTriggerExpiry.EndOfTurn,
                effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.TriggeringEntity)
            )
        manaAbility = true
        timing = TimingRule.ManaAbility
        description = "{T}: Add {G}. When you next cast a Bird creature spell this turn, it enters " +
            "with an additional +1/+1 counter on it."
    }

    // {2}{G}{G}, {T}: Create a 2/2 green Bird creature token with "Whenever a land you control
    // enters, this token gets +1/+0 until end of turn."
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{G}{G}"), Costs.Tap)
        effect = Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Bird"),
            imageUri = "https://cards.scryfall.io/normal/front/1/f/1fbc471d-5948-47fc-b7cc-81cc13a4cd15.jpg?1783906133",
            triggeredAbilities = listOf(
                TriggeredAbility.create(
                    trigger = Triggers.a(GameObjectFilter.Land.youControl()).enters(),
                    effect = Effects.ModifyStats(1, 0, EffectTarget.Self),
                ),
            ),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "462"
        artist = "Winona Nelson"
        imageUri = "https://cards.scryfall.io/normal/front/4/f/4fcd08ad-4dac-4236-9030-f59e473b3ec7.jpg?1783904649"

        ruling(
            "2025-10-02",
            "The legendary creature must already be on the battlefield as the land enters the " +
                "battlefield. If it enters the battlefield at the same time, the land will enter tapped."
        )
    }
}
