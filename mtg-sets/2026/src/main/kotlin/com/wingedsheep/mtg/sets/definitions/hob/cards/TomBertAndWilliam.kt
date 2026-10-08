package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Tom, Bert, and William
 * {3}{B}{G}
 * Legendary Creature — Troll
 * 5/5
 *
 * {1}, Sacrifice another creature: Draw cards equal to the sacrificed creature's power, then
 * discard a card.
 * When Tom, Bert, and William die, if they were a creature, return them to the battlefield.
 * They're an artifact. (They're no longer a creature.)
 *
 * The draw count is [EffectTarget.SacrificedAsCost] — whose LKI policy is `LIVE_THEN_LKI`, so the
 * power read is the one the creature last had on the battlefield, per the card's ruling ("use the
 * sacrificed creature's power as it last existed on the battlefield"). A sacrificed 0-power
 * creature draws nothing but the discard still happens: "then discard a card" is not conditional.
 *
 * The dies trigger is a self-recursion loop guarded by an intervening "if". The guard has to read
 * *last-known* information: once the trigger is considered, Tom is a card in the graveyard wearing
 * its printed `Legendary Creature — Troll` type line again, so asking the live entity would answer
 * "creature" on both deaths and the pair would recur forever. [Conditions.TriggeringEntityWas]
 * reads the projected type line captured when the permanent left the battlefield (CR 603.10), which
 * is where [BecomeArtifactEffect]'s type change shows up — so the second death sees `ARTIFACT`, the
 * guard fails, and the loop stops after exactly one return.
 *
 * [BecomeArtifactEffect] sets the card types to ARTIFACT alone, which drops CREATURE and, with it,
 * the Troll creature type (CR 205.1a). Abilities are kept (`loseAllAbilities = false`) — nothing on
 * the card takes them away, so the returned artifact still carries the sacrifice outlet and the dies
 * trigger. Colors are kept too (`colors = null`, against the parameter's colorless default): the
 * card changes only its type. [Duration.Permanent] lasts until the permanent next leaves the
 * battlefield, so the type change is still in force at the moment the snapshot for that second
 * death is taken.
 */
val TomBertAndWilliam = card("Tom, Bert, and William") {
    manaCost = "{3}{B}{G}"
    colorIdentity = "BG"
    typeLine = "Legendary Creature — Troll"
    power = 5
    toughness = 5
    oracleText = "{1}, Sacrifice another creature: Draw cards equal to the sacrificed creature's " +
        "power, then discard a card.\n" +
        "When Tom, Bert, and William die, if they were a creature, return them to the battlefield. " +
        "They're an artifact."

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{1}"),
            Costs.SacrificeAnother(GameObjectFilter.Creature)
        )
        effect = Effects.DrawCards(
            DynamicAmounts.powerOf(EffectTarget.SacrificedAsCost())
        ) then
            Effects.Discard(1)
        description = "{1}, Sacrifice another creature: Draw cards equal to the sacrificed " +
            "creature's power, then discard a card."
    }

    triggeredAbility {
        trigger = Triggers.self.dies()
        interveningIf = Conditions.TriggeringEntityWas(GameObjectFilter.Creature)
        effect = Effects.Move(
            target = EffectTarget.Self,
            destination = Zone.BATTLEFIELD,
            fromZone = Zone.GRAVEYARD
        ) then
            Effects.BecomeArtifact(
                target = EffectTarget.Self,
                cardTypes = setOf(CardType.ARTIFACT.name),
                subtypes = emptySet(),
                // `null` keeps the printed colors — the card only changes its type, so the returned
                // artifact is still black-green. The parameter's default is `emptySet()` (colorless).
                colors = null,
                loseAllAbilities = false,
                duration = Duration.Permanent
            )
        description = "When Tom, Bert, and William die, if they were a creature, return them to " +
            "the battlefield. They're an artifact."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "169"
        artist = "Leonardo Borazio"
        imageUri = "https://cards.scryfall.io/normal/front/2/1/211a9764-3c60-46ba-bb53-e6692640ec8f.jpg?1783902784"
        ruling("2026-06-29", "Use the sacrificed creature's power as it last existed on the battlefield to determine how many cards you draw.")
    }
}
