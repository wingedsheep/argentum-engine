package com.wingedsheep.mtg.sets.definitions.lci.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.MultiplyTokenCreation
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.TransformEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ojer Taq, Deepest Foundation // Temple of Civilization (The Lost Caverns of Ixalan)
 * {4}{W}{W}
 * Legendary Creature — God // Land
 *
 * Front — Ojer Taq, Deepest Foundation (6/6, Vigilance)
 *   If one or more creature tokens would be created under your control, three times that many
 *   of those tokens are created instead.
 *   When Ojer Taq dies, return it to the battlefield tapped and transformed under its owner's
 *   control.
 *
 * Back — Temple of Civilization (Land)
 *   {T}: Add {W}.
 *   {2}{W}, {T}: Transform this land. Activate only if you attacked with three or more creatures
 *   this turn and only as a sorcery.
 *
 * Implementation:
 *  - Token tripling is [MultiplyTokenCreation]`(factor = 3)` — the Doubling Season replacement
 *    generalized to any factor. Its `appliesTo` defaults to `TokenCreationEvent(controller = You)`;
 *    the count multiplier runs in `CreateTokenExecutor`, which is the executor for *creature*
 *    tokens (it always builds a "… Creature" type line — Treasure/Clue/Map and other predefined
 *    tokens use a different executor that isn't multiplied), so the effect's scope coincides with
 *    the oracle's "creature tokens".
 *  - Dies-return uses the shared [Effects.ReturnSelfFromGraveyardTransformed]`(tapped = true)`
 *    wired to [Triggers.Dies].
 *  - Back land: `{T}: Add {W}` mana ability + a `{2}{W}, {T}` sorcery-speed [TransformEffect]
 *    gated on [Conditions.YouAttackedWithCreaturesThisTurn]`(Creature, 3)`.
 */

private val OjerTaqDeepestFoundationFront = card("Ojer Taq, Deepest Foundation") {
    manaCost = "{4}{W}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — God"
    power = 6
    toughness = 6
    oracleText = "Vigilance\n" +
        "If one or more creature tokens would be created under your control, three times that " +
        "many of those tokens are created instead.\n" +
        "When Ojer Taq dies, return it to the battlefield tapped and transformed under its " +
        "owner's control."

    keywords(Keyword.VIGILANCE)

    replacementEffect(MultiplyTokenCreation(factor = 3))

    triggeredAbility {
        trigger = Triggers.Dies
        effect = Effects.ReturnSelfFromGraveyardTransformed(tapped = true)
        description = "When Ojer Taq dies, return it to the battlefield tapped and transformed " +
            "under its owner's control."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "26"
        artist = "Cristi Balanescu"
        imageUri = "https://cards.scryfall.io/normal/front/1/c/1ca79dd4-67fc-496c-96fc-489b039c4932.jpg?1782694591"
    }
}

private val TempleOfCivilization = card("Temple of Civilization") {
    manaCost = ""
    colorIdentity = "W"
    typeLine = "Land"
    oracleText = "(Transforms from Ojer Taq, Deepest Foundation.)\n" +
        "{T}: Add {W}.\n" +
        "{2}{W}, {T}: Transform this land. Activate only if you attacked with three or more " +
        "creatures this turn and only as a sorcery."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.WHITE, 1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{W}"), Costs.Tap)
        effect = TransformEffect(EffectTarget.Self)
        timing = TimingRule.SorcerySpeed
        restrictions = listOf(
            ActivationRestriction.OnlyIfCondition(
                Conditions.YouAttackedWithCreaturesThisTurn(GameObjectFilter.Creature, 3)
            )
        )
        description = "Transform this land. Activate only if you attacked with three or more " +
            "creatures this turn and only as a sorcery."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "26"
        artist = "Cristi Balanescu"
        flavorText = "Chimil gave the Oltec life. Ojer Taq taught them how to live together."
        imageUri = "https://cards.scryfall.io/normal/back/1/c/1ca79dd4-67fc-496c-96fc-489b039c4932.jpg?1782694591"
    }
}

val OjerTaqDeepestFoundation: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = OjerTaqDeepestFoundationFront,
    backFace = TempleOfCivilization,
)
