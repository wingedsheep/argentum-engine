package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Supertype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CopyExceptions
import com.wingedsheep.sdk.scripting.effects.EnteringAttackDefender
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Shaun, Father of Synths — the copy enters attacking a defender its controller picks as it
 * enters (CR 508.4: `EnteringAttackDefender.CONTROLLER_CHOOSES`), not necessarily the one Shaun
 * attacks. The leaves trigger exiles every Synth token you control, not only the ones Shaun made.
 */
val ShaunFatherOfSynths = card("Shaun, Father of Synths") {
    manaCost = "{3}{U}{R}"
    colorIdentity = "UR"
    typeLine = "Legendary Creature — Human Scientist"
    power = 3
    toughness = 4
    oracleText = "Whenever you attack, you may create a tapped and attacking token that's a copy of " +
        "target attacking legendary creature you control other than Shaun, except it's not legendary " +
        "and it's a Synth artifact creature in addition to its other types.\n" +
        "When Shaun leaves the battlefield, exile all Synth tokens you control."

    triggeredAbility {
        trigger = Triggers.you.attacks()
        val original = target(
            TargetFilter(GameObjectFilter.Creature.legendary().attacking().youControl()).other()
        )
        effect = Effects.May(
            Effects.CreateTokenCopyOfTarget(
                target = original,
                tapped = true,
                attacking = true,
                attackDefender = EnteringAttackDefender.CONTROLLER_CHOOSES,
                exceptions = CopyExceptions(
                    removedSupertypes = setOf(Supertype.LEGENDARY),
                    addedCardTypes = setOf(CardType.ARTIFACT, CardType.CREATURE),
                    addedSubtypes = setOf(Subtype("Synth")),
                ),
            )
        )
    }

    triggeredAbility {
        trigger = Triggers.self.leaves()
        effect = Effects.Pipeline {
            exile(gather(GameObjectFilter.Permanent.token().withSubtype("Synth").youControl()))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "119"
        artist = "Zuzanna Wużyk"
        imageUri = "https://cards.scryfall.io/normal/front/6/8/68af449f-3c30-43ea-be60-d85061d7adbd.jpg?1783912382"
        ruling("2024-03-08", "Except for the noted exceptions, the token copies exactly what was printed on the original creature and nothing else (unless that creature is copying something else or is a token; see below). It doesn't copy whether that creature is tapped or untapped, whether it has any counters on it or Auras and Equipment attached to it, or any non-copy effects that have changed its power, toughness, types, color, and so on.")
        ruling("2024-03-08", "If the copied creature has {X} in its mana cost, X is 0.")
        ruling("2024-03-08", "If the copied creature is a token, the token that's created copies the original characteristics of that token as stated by the effect that created that token, with the noted exceptions.")
        ruling("2024-03-08", "If the copied creature is copying something else, then the token enters the battlefield as whatever that creature copied, with the noted exceptions.")
        ruling("2024-03-08", "Any enters-the-battlefield abilities of the copied creature will trigger when the token enters the battlefield. Any \"as [this creature] enters the battlefield\" or \"[this creature] enters the battlefield with\" abilities of the copied creature will also work.")
        ruling("2024-03-08", "Although the token is attacking, it was never declared as an attacking creature. This means that abilities that trigger whenever a creature attacks won't trigger when it enters the battlefield attacking.")
        ruling("2024-03-08", "You declare which player, planeswalker, or battle the token is attacking as you put it onto the battlefield. It doesn't have to be attacking the same player, planeswalker, or battle that Shaun is attacking.")
        ruling("2024-03-08", "Shaun's last ability will exile all Synth tokens you control, not just Synth tokens created by its first ability.")
    }
}
