package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.SacrificeSelfEffect
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Flaxen Intruder // Welcome Home
 * {G}
 * Creature — Human Berserker
 * 1/2
 *
 * Whenever this creature deals combat damage to a player, you may sacrifice it. When you do,
 * destroy target artifact or enchantment.
 *
 * Adventure: Welcome Home — {5}{G}{G}, Sorcery — Adventure
 * Create three 2/2 green Bear creature tokens.
 *
 * The "when you do" is a CR 603.12 reflexive trigger: the combat-damage trigger has no target, and
 * the artifact or enchantment is chosen only once the Intruder has been sacrificed (as
 * `neo/cards/KappaTechWrecker.kt`). The sacrifice can only be performed while the Intruder is still
 * on the battlefield, so the whole may-clause is gated on `SourceInZone(BATTLEFIELD)` — a creature
 * bounced or killed in response can't be sacrificed, and nothing is destroyed.
 */
val FlaxenIntruder = card("Flaxen Intruder") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Berserker"
    power = 1
    toughness = 2
    oracleText = "Whenever this creature deals combat damage to a player, you may sacrifice it. " +
        "When you do, destroy target artifact or enchantment."

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.If(
            Conditions.SourceInZone(Zone.BATTLEFIELD),
            Effects.ReflexiveTrigger(
                action = SacrificeSelfEffect,
                optional = true,
                descriptionOverride = "You may sacrifice this creature. When you do, destroy target " +
                    "artifact or enchantment."
            ) {
                val victim = target(TargetFilter.ArtifactOrEnchantment)
                effect = Effects.Destroy(victim)
            }
        )
        description = "Whenever this creature deals combat damage to a player, you may sacrifice it. " +
            "When you do, destroy target artifact or enchantment."
    }

    adventure("Welcome Home") {
        manaCost = "{5}{G}{G}"
        typeLine = "Sorcery — Adventure"
        oracleText = "Create three 2/2 green Bear creature tokens. (Then exile this card. You may " +
            "cast the creature later from exile.)"
        spell {
            effect = Effects.CreateToken(
                power = 2,
                toughness = 2,
                colors = setOf(Color.GREEN),
                creatureTypes = setOf("Bear"),
                count = 3
            )
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "155"
        artist = "Gabor Szikszai"
        flavorText = "The middle blade was just right."
        imageUri = "https://cards.scryfall.io/normal/front/0/6/06bd1ad2-fb5d-4aef-87d1-13a341c686fa.jpg?1783932614"
        ruling(
            "2019-10-04",
            "Flaxen Intruder's triggered ability goes on the stack without a target. While that ability " +
                "is resolving, you may sacrifice it. When you do, the reflexive triggered ability triggers " +
                "and you pick a target artifact or enchantment to be destroyed. This is different from " +
                "effects that say \"If you do . . .\" in that players may take actions after you've " +
                "sacrificed Flaxen Intruder but before the artifact or enchantment is destroyed."
        )
    }
}
