package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.LoseLifeEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Gollum the Abandoned — The Hobbit #72
 * {1}{B} · Legendary Creature — Halfling Horror · Uncommon
 * 2/2
 *
 * Gollum can't block.
 * When Gollum enters, exile up to one target card from an opponent's graveyard. Each opponent
 * loses 2 life.
 * {2}, Sacrifice an artifact or creature: Return this card from your graveyard to your hand.
 * Activate only as a sorcery.
 *
 * Modeling notes:
 *  - The exile half is "up to one target", so the ETB is castable into an empty board and the
 *    life loss still happens with no target chosen. It does *not* happen if the one chosen target
 *    becomes illegal before resolution — the ability is then countered for having no legal targets
 *    (CR 608.2b), which is the printed behaviour.
 *  - The recursion ability lives in the graveyard (`activateFromZone`), so the sacrificed artifact
 *    or creature is never Gollum himself. `TimingRule.SorcerySpeed` carries "activate only as a
 *    sorcery".
 */
val GollumTheAbandoned = card("Gollum the Abandoned") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Halfling Horror"
    power = 2
    toughness = 2
    oracleText = "Gollum can't block.\n" +
        "When Gollum enters, exile up to one target card from an opponent's graveyard. " +
        "Each opponent loses 2 life.\n" +
        "{2}, Sacrifice an artifact or creature: Return this card from your graveyard to your hand. " +
        "Activate only as a sorcery."

    staticAbility {
        ability = CantBlock()
    }

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val exiled = target(
            "card in an opponent's graveyard",
            TargetObject(
                optional = true,
                filter = TargetFilter(GameObjectFilter.Any.ownedByOpponent(), zone = Zone.GRAVEYARD)
            )
        )
        effect = Effects.Move(exiled, Zone.EXILE) then
            LoseLifeEffect(2, EffectTarget.PlayerRef(Player.EachOpponent))
        description = "When Gollum the Abandoned enters, exile up to one target card from an " +
            "opponent's graveyard. Each opponent loses 2 life."
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{2}"),
            Costs.Sacrifice(GameObjectFilter.CreatureOrArtifact)
        )
        effect = Effects.Move(EffectTarget.Self, Zone.HAND)
        activateFromZone = Zone.GRAVEYARD
        timing = TimingRule.SorcerySpeed
        description = "{2}, Sacrifice an artifact or creature: Return this card from your " +
            "graveyard to your hand. Activate only as a sorcery."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "72"
        artist = "Andrea Piparo"
        flavorText = "\"Where iss it? Losst it is, my precious!\""
        imageUri = "https://cards.scryfall.io/normal/front/5/0/50d91ef3-6f5d-4255-8d47-be731b5dad30.jpg?1784733916"
    }
}
