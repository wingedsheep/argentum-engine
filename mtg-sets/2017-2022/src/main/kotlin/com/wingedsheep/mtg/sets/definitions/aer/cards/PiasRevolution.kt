package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Pia's Revolution — Aether Revolt #91
 * {2}{R} · Enchantment
 * Whenever a nontoken artifact is put into your graveyard from the battlefield, return that card to
 * your hand unless target opponent has this enchantment deal 3 damage to them.
 *
 * "Put into your graveyard" is ownership, not control (ruling), so the subject is
 * `Artifact.nontoken().ownedByYou()` on the dies event. The punisher choice is a `May` answered by
 * the targeted opponent (`decisionMaker`, as Risk Factor): accepting has the enchantment deal 3 damage
 * to them; declining runs `otherwise` — the return, gated on the card still being in the graveyard,
 * so a card that left it in response stays where it is (ruling). With no legal opponent to target,
 * the ability is removed from the stack and nothing happens (ruling).
 */
val PiasRevolution = card("Pia's Revolution") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment"
    oracleText = "Whenever a nontoken artifact is put into your graveyard from the battlefield, return " +
        "that card to your hand unless target opponent has this enchantment deal 3 damage to them."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Artifact.nontoken().ownedByYou()).dies()
        val opponent = target(Targets.Opponent)
        effect = Effects.May(
            effect = Effects.DealDamage(3, opponent, damageSource = EffectTarget.Self),
            otherwise = Effects.Move(EffectTarget.TriggeringEntity, Zone.HAND, fromZone = Zone.GRAVEYARD),
            decisionMaker = opponent,
            // Shown to the opponent being asked, so it reads from their perspective.
            descriptionOverride = "Have Pia's Revolution deal 3 damage to you? If you don't, its " +
                "controller returns the artifact card to their hand.",
        )
        description = "Whenever a nontoken artifact is put into your graveyard from the battlefield, " +
            "return that card to your hand unless target opponent has this enchantment deal 3 damage to them."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "91"
        artist = "Clint Cearley"
        flavorText = "Pia's passion was directed at the big picture—at making real, lasting change."
        imageUri = "https://cards.scryfall.io/normal/front/5/1/51da6ff6-4d81-488e-83ff-8758f6c7bb9f.jpg?1783936751"
        ruling(
            "2017-02-09",
            "If a nontoken artifact is put into your graveyard at the same time as Pia's Revolution, " +
                "Pia's Revolution's ability triggers."
        )
        ruling(
            "2017-02-09",
            "It doesn't matter who controlled the artifact when it was put into your graveyard. Pia's " +
                "Revolution's ability will trigger if you own that artifact."
        )
        ruling(
            "2017-02-09",
            "The target opponent chooses whether to have Pia's Revolution deal 3 damage to them as its " +
                "ability resolves. You won't return the card to your hand if that player chooses to be dealt " +
                "damage or if the card leaves the graveyard before the ability resolves."
        )
        ruling(
            "2017-02-09",
            "If there are no legal targets for Pia's Revolution's ability (perhaps because each of your " +
                "opponents has hexproof), it will be removed from the stack with no effect. No one may " +
                "choose to be dealt damage and you won't return the artifact card to your hand."
        )
    }
}
