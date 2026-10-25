package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Acererak the Archlich
 * {2}{B}
 * Legendary Creature — Zombie Wizard
 * 5/5
 * When Acererak enters, if you haven't completed Tomb of Annihilation, return Acererak to its
 * owner's hand and venture into the dungeon.
 * Whenever Acererak attacks, for each opponent, you create a 2/2 black Zombie creature token unless
 * that player sacrifices a creature of their choice.
 *
 * The Tomb check is an intervening-if (CR 603.4): checked as the ability would trigger and again on
 * resolution. The venture happens even if Acererak has already left the battlefield (ruling) — the
 * bounce simply finds nothing to move.
 *
 * The attack trigger is The Long Reach of Night's per-opponent punisher: `ForEachPlayer` rebinds
 * `Controller` to each opponent in turn, who chooses whether to sacrifice; the token goes to
 * Acererak's controller via `ControllerOfSource`, not to the iterated opponent. An opponent with no
 * creature can't pay, so you create the token.
 */
val AcererakTheArchlich = card("Acererak the Archlich") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Zombie Wizard"
    power = 5
    toughness = 5
    oracleText = "When Acererak enters, if you haven't completed Tomb of Annihilation, return Acererak " +
        "to its owner's hand and venture into the dungeon.\n" +
        "Whenever Acererak attacks, for each opponent, you create a 2/2 black Zombie creature token " +
        "unless that player sacrifices a creature of their choice."

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.Not(Conditions.CompletedDungeon(Player.You, "Tomb of Annihilation"))
        effect = Effects.ReturnToHand(EffectTarget.Self) then Effects.VentureIntoTheDungeon()
        description = "When Acererak enters, if you haven't completed Tomb of Annihilation, return " +
            "Acererak to its owner's hand and venture into the dungeon."
    }

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.ForEachPlayer(
            Player.EachOpponent,
            Effects.PayOrSuffer(
                cost = Costs.pay.Sacrifice(GameObjectFilter.Creature),
                suffer = Effects.CreateToken(
                    power = 2,
                    toughness = 2,
                    colors = setOf(Color.BLACK),
                    creatureTypes = setOf("Zombie"),
                    controller = EffectTarget.PlayerRef(Player.ControllerOfSource),
                    imageUri = "https://cards.scryfall.io/normal/front/0/b/0b08d210-01cb-46c5-9150-4dfb47f50ae7.jpg?1783926346"
                ),
                player = EffectTarget.Controller,
            )
        )
        description = "Whenever Acererak attacks, for each opponent, you create a 2/2 black Zombie " +
            "creature token unless that player sacrifices a creature of their choice."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "87"
        artist = "Andrey Kuzinskiy"
        imageUri = "https://cards.scryfall.io/normal/front/d/d/dd52d0bd-3abd-401c-9f56-ee911613da3b.jpg?1783926501"
        ruling("2021-07-23", "Acererak the Archlich has an intervening if clause in its first triggered ability. If you have completed Tomb of Annihilation, the ability won't trigger at all. If it does trigger, but you somehow complete Tomb of Annihilation while the ability is on the stack, then the ability won't do anything. You won't return Acererak to its owner's hand and you won't venture into the dungeon.")
        ruling("2021-07-23", "If Acererak is no longer on the battlefield as its first ability resolves and you haven't completed Tomb of Annihilation, you will still venture into the dungeon.")
        ruling("2021-07-23", "In a multiplayer game, each opponent decides in turn order whether or not they will sacrifice a creature and which one they will sacrifice. Each opponent knows the choices made before they make their own choice. Then, once all opponents have chosen, creatures are sacrificed and tokens are created simultaneously.")
    }
}
