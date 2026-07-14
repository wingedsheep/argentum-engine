package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.training
import com.wingedsheep.sdk.dsl.trainingTriggeredAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect

/**
 * Torens, Fist of the Angels
 * {1}{G}{W}
 * Legendary Creature — Human Cleric
 * 2/2
 * Training (Whenever this creature attacks with another creature with greater power, put a
 * +1/+1 counter on this creature.)
 * Whenever you cast a creature spell, create a 1/1 green and white Human Soldier creature token
 * with training.
 *
 * [training] gives Torens itself the keyword + attack trigger. The second ability makes a token
 * that *intrinsically* has Training: [CreateTokenEffect] carries both `keywords`
 * (`Keyword.TRAINING`, for the display badge) and `triggeredAbilities`
 * ([trainingTriggeredAbility], the behavior). `CreateTokenExecutor` grants token-borne triggered
 * abilities `Duration.Permanent`, so the token trains on its own when it later attacks alongside
 * a creature of greater power — proving Training travels on a token definition, not just a card.
 */
val Torens = card("Torens, Fist of the Angels") {
    manaCost = "{1}{G}{W}"
    colorIdentity = "GW"
    typeLine = "Legendary Creature — Human Cleric"
    power = 2
    toughness = 2
    oracleText = "Training (Whenever this creature attacks with another creature with greater " +
        "power, put a +1/+1 counter on this creature.)\n" +
        "Whenever you cast a creature spell, create a 1/1 green and white Human Soldier creature " +
        "token with training."

    training()

    triggeredAbility {
        trigger = Triggers.YouCastCreature
        effect = CreateTokenEffect(
            // count defaults to Fixed(1); the primary (DynamicAmount) constructor is required here
            // because only it carries `triggeredAbilities` (the token's own Training behavior).
            power = 1,
            toughness = 1,
            colors = setOf(Color.GREEN, Color.WHITE),
            creatureTypes = setOf("Human", "Soldier"),
            keywords = setOf(Keyword.TRAINING),
            triggeredAbilities = listOf(trainingTriggeredAbility()),
            imageUri = "https://cards.scryfall.io/normal/front/5/f/5f3c4810-7359-42b7-905f-4845f6d1daf6.jpg?1783924693",
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "249"
        artist = "Justine Cruz"
        imageUri = "https://cards.scryfall.io/normal/front/b/d/bd111dc9-b8de-4e92-a63d-75a961b2db3b.jpg?1783924787"
    }
}
