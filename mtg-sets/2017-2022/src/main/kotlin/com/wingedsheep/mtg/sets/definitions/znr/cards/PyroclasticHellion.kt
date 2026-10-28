package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Pyroclastic Hellion — Zendikar Rising #152
 * {4}{R} · Creature — Hellion · 4/5
 *
 * When this creature enters, you may return a land you control to its owner's hand. When you do,
 * this creature deals 2 damage to each opponent.
 *
 * The ETB trigger doesn't target; the land is chosen on resolution (Zell Dincht's
 * gather → choose → toHand shape). Returning it is the optional action of a reflexive trigger, so
 * the 2 damage to each opponent goes on the stack only if a land actually went back — with no land
 * to return, the action is infeasible and nothing triggers.
 */
val PyroclasticHellion = card("Pyroclastic Hellion") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Hellion"
    power = 4
    toughness = 5
    oracleText = "When this creature enters, you may return a land you control to its owner's hand. " +
        "When you do, this creature deals 2 damage to each opponent."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.ReflexiveTrigger(
            action = Effects.Pipeline {
                val lands = gather(CardSource.BattlefieldMatching(filter = GameObjectFilter.Land, player = Player.You))
                val returned = chooseExactly(
                    1,
                    from = lands,
                    prompt = "Return a land you control to its owner's hand",
                    useTargetingUI = true
                )
                toHand(returned)
            },
            reflexiveEffect = Effects.DealDamage(2, EffectTarget.PlayerRef(Player.EachOpponent)),
            optional = true,
            descriptionOverride = "When this creature enters, you may return a land you control to its owner's hand. " +
                "When you do, this creature deals 2 damage to each opponent."
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "152"
        artist = "Simon Dominic"
        flavorText = "\"The difference between a hellion and a volcano is that volcanoes don't get hungry.\"\n" +
            "—Sachir, Akoum Expeditionary House"
        imageUri = "https://cards.scryfall.io/normal/front/5/9/5929c89b-8c61-47f5-a8ec-b64bf41e9d5b.jpg?1783929354"
    }
}
