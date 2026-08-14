package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.ConditionalOnCollectionEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.MoveType
import com.wingedsheep.sdk.scripting.effects.RevealHandEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.effects.TransformEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Concealing Curtains // Revealing Eye (Innistrad: Crimson Vow)
 * {B}
 * Creature — Wall // Creature — Eye Horror
 *
 * Front — Concealing Curtains (0/4)
 *   Defender
 *   {2}{B}: Transform this creature. Activate only as a sorcery.
 *
 * Back — Revealing Eye (3/4)
 *   Menace
 *   When this creature transforms into Revealing Eye, target opponent reveals their hand. You may
 *   choose a nonland card from it. If you do, that player discards that card, then draws a card.
 *
 * The back's transforms-into trigger ([Triggers.TransformsToBack]) runs the Duress reveal pipeline:
 * [RevealHandEffect] on a target opponent, [GatherCardsEffect] over that opponent's hand,
 * [SelectFromCollectionEffect] as ChooseUpTo 1 nonland ("you may choose"), and a
 * [ConditionalOnCollectionEffect] so the discard-then-draw only happens "if you do" (Oildeep
 * Gearhulk's idiom) — and the *targeted opponent* is the one who discards and draws. The back is a
 * transformed face with no mana cost, so its color comes from a color indicator (CR 204):
 * `colorIndicator = "B"`.
 */

private val ConcealingCurtainsFront = card("Concealing Curtains") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Creature — Wall"
    power = 0
    toughness = 4
    oracleText = "Defender\n" +
        "{2}{B}: Transform this creature. Activate only as a sorcery."

    keywords(Keyword.DEFENDER)

    activatedAbility {
        cost = Costs.Mana("{2}{B}")
        effect = TransformEffect(EffectTarget.Self)
        timing = TimingRule.SorcerySpeed
        description = "Transform this creature. Activate only as a sorcery."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "101"
        artist = "Brian Valeza"
        imageUri = "https://cards.scryfall.io/normal/front/6/1/612b2e6e-fe8d-49ad-b845-6fa7fa59ffd1.jpg?1783924877"
    }
}

private val RevealingEye = card("Revealing Eye") {
    manaCost = ""
    colorIdentity = "B"
    colorIndicator = "B" // Transformed back face, no mana cost (CR 204).
    typeLine = "Creature — Eye Horror"
    power = 3
    toughness = 4
    oracleText = "Menace\n" +
        "When this creature transforms into Revealing Eye, target opponent reveals their hand. You " +
        "may choose a nonland card from it. If you do, that player discards that card, then draws a card."

    keywords(Keyword.MENACE)

    triggeredAbility {
        trigger = Triggers.TransformsToBack
        val opponent = target("target opponent", Targets.Opponent)
        effect = Effects.Composite(
            RevealHandEffect(opponent),
            GatherCardsEffect(
                source = CardSource.FromZone(Zone.HAND, Player.ContextPlayer(0)),
                storeAs = "revealedHand",
            ),
            SelectFromCollectionEffect(
                from = "revealedHand",
                selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(1)),
                chooser = Chooser.Controller,
                filter = GameObjectFilter.Nonland,
                storeSelected = "chosenCard",
                prompt = "You may choose a nonland card for that player to discard",
                alwaysPrompt = true,
                showAllCards = true,
            ),
            ConditionalOnCollectionEffect(
                collection = "chosenCard",
                ifNotEmpty = Effects.Composite(
                    MoveCollectionEffect(
                        from = "chosenCard",
                        destination = CardDestination.ToZone(Zone.GRAVEYARD, Player.ContextPlayer(0)),
                        moveType = MoveType.Discard,
                    ),
                    Effects.DrawCards(1, opponent),
                ),
            ),
        )
        description = "When this creature transforms into Revealing Eye, target opponent reveals " +
            "their hand. You may choose a nonland card from it. If you do, that player discards that " +
            "card, then draws a card."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "101"
        artist = "Brian Valeza"
        imageUri = "https://cards.scryfall.io/normal/back/6/1/612b2e6e-fe8d-49ad-b845-6fa7fa59ffd1.jpg?1783924877"
    }
}

val ConcealingCurtains: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = ConcealingCurtainsFront,
    backFace = RevealingEye,
)
