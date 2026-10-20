package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.EffectChoice
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Gabriel Angelfire
 * {3}{G}{G}{W}{W}
 * Legendary Creature — Angel
 * 4/4
 *
 * At the beginning of your upkeep, choose flying, first strike, trample, or rampage 3. Gabriel
 * Angelfire gains that ability until your next upkeep.
 *
 * The choice is made as the trigger resolves (not a modal ability), so it's an
 * [Effects.ChooseAction]. Each grant lasts [Duration.UntilYourNextUpkeep]: it expires as the next
 * upkeep begins, before that upkeep's new trigger resolves, so the abilities never accumulate.
 * Rampage 3 is a granted `RAMPAGE_3` keyword, from which the engine derives the rampage trigger.
 */
val GabrielAngelfire = card("Gabriel Angelfire") {
    manaCost = "{3}{G}{G}{W}{W}"
    colorIdentity = "GW"
    typeLine = "Legendary Creature — Angel"
    power = 4
    toughness = 4
    oracleText = "At the beginning of your upkeep, choose flying, first strike, trample, or rampage 3. " +
        "Gabriel Angelfire gains that ability until your next upkeep. (Whenever a creature with " +
        "rampage 3 becomes blocked, it gets +3/+3 until end of turn for each creature blocking it " +
        "beyond the first.)"

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.ChooseAction(listOf(
            EffectChoice(
                label = "Flying",
                effect = Effects.GrantKeyword(Keyword.FLYING, EffectTarget.Self, Duration.UntilYourNextUpkeep),
            ),
            EffectChoice(
                label = "First strike",
                effect = Effects.GrantKeyword(Keyword.FIRST_STRIKE, EffectTarget.Self, Duration.UntilYourNextUpkeep),
            ),
            EffectChoice(
                label = "Trample",
                effect = Effects.GrantKeyword(Keyword.TRAMPLE, EffectTarget.Self, Duration.UntilYourNextUpkeep),
            ),
            EffectChoice(
                label = "Rampage 3",
                effect = Effects.GrantRampage(3, EffectTarget.Self, Duration.UntilYourNextUpkeep),
            ),
        ))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "226"
        artist = "Daniel Gelon"
        imageUri = "https://cards.scryfall.io/normal/front/3/b/3b4ce350-b6ed-4e0e-8c70-efc6e5f18a5d.jpg?1783948040"
    }
}
