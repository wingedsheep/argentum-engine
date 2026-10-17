package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.SuccessCriterion
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreatureOrPlaneswalker

/**
 * Inventive Iteration // Living Breakthrough
 * {3}{U} — Enchantment — Saga // Enchantment Creature — Moonfolk 3/3
 *
 * Front — Inventive Iteration:
 *   I — Return up to one target creature or planeswalker to its owner's hand.
 *   II — Return an artifact card from your graveyard to your hand. If you can't, draw a card.
 *   III — Exile this Saga, then return it to the battlefield transformed under your control.
 *
 * Back — Living Breakthrough:
 *   Flying
 *   Whenever you cast a spell, your opponents can't cast spells with the same mana value as that
 *   spell until your next turn.
 *
 * The ban's mana value is locked in as the trigger resolves (the cast-ban executor fixes a dynamic
 * mana value then), so it holds after the triggering spell has left the stack.
 */
private val LivingBreakthrough = card("Living Breakthrough") {
    manaCost = ""
    colorIdentity = "U"
    typeLine = "Enchantment Creature — Moonfolk"
    oracleText = "Flying\n" +
        "Whenever you cast a spell, your opponents can't cast spells with the same mana value as that " +
        "spell until your next turn."
    power = 3
    toughness = 3

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.you.casts()
        effect = Effects.CantCastSpells(
            target = EffectTarget.PlayerRef(Player.EachOpponent),
            duration = Duration.UntilYourNextTurn,
            spellFilter = GameObjectFilter.Any.manaValueEqualsDynamic(DynamicAmounts.triggeringManaValue())
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "57"
        artist = "Néstor Ossandón Leal"
        flavorText = "\"While they squabble below, let us rise above.\"\n—Emire, director of the Saiba Futurists"
        imageUri = "https://cards.scryfall.io/normal/back/e/a/ea6d3242-8059-4203-aa06-58a6a3925eda.jpg?1783923909"
    }
}

private val InventiveIterationFront = card("Inventive Iteration") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter.)\n" +
        "I — Return up to one target creature or planeswalker to its owner's hand.\n" +
        "II — Return an artifact card from your graveyard to your hand. If you can't, draw a card.\n" +
        "III — Exile this Saga, then return it to the battlefield transformed under your control."

    sagaChapter(1) {
        val permanent = target(TargetCreatureOrPlaneswalker(optional = true))
        effect = Effects.ReturnToHand(permanent)
    }

    sagaChapter(2) {
        effect = Effects.IfYouDo(
            action = Effects.Pipeline {
                val pool = gather(CardSource.FromZone(Zone.GRAVEYARD, Player.You, GameObjectFilter.Artifact))
                // Named: the "if you can't" check below reads it from outside the pipeline.
                val returned = chooseExactly(
                    1,
                    from = pool,
                    prompt = "Return an artifact card from your graveyard to your hand",
                    selectedLabel = "Return to hand",
                    name = "returned"
                )
                toHand(returned)
            },
            then = Effects.Nothing,
            otherwise = Effects.DrawCards(1),
            successCriterion = SuccessCriterion.CollectionNonEmpty("returned")
        )
    }

    sagaChapter(3) {
        effect = Effects.ExileAndReturnTransformed()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "57"
        artist = "Néstor Ossandón Leal"
        imageUri = "https://cards.scryfall.io/normal/front/e/a/ea6d3242-8059-4203-aa06-58a6a3925eda.jpg?1783923909"
        ruling("2022-02-18", "Living Breakthrough's triggered ability will resolve before the spell that caused it to trigger. Notably, the ability has no effect until it resolves, so your opponents can respond to it by casting instant spells with the same mana value as the original spell.")
        ruling("2022-02-18", "Living Breakthrough's triggered ability won't affect spells that a player has already cast, even if those spells haven't resolved yet.")
    }
}

val InventiveIteration: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = InventiveIterationFront,
    backFace = LivingBreakthrough,
)
