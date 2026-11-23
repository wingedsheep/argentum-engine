package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.EffectChoice

/**
 * Angelic Skirmisher — Gatecrash #3
 * {4}{W}{W} · Creature — Angel · 4 / 4
 *
 * Flying
 * At the beginning of each combat, choose first strike, vigilance, or lifelink. Creatures you
 * control gain that ability until end of turn.
 *
 * Triggers on every player's beginning of combat. The choice is made as the trigger resolves (not a
 * modal ability), so it's an [Effects.ChooseAction]; the group grant gathers the creatures you
 * control at that moment, so creatures that arrive later in the turn don't gain the ability.
 */
val AngelicSkirmisher = card("Angelic Skirmisher") {
    manaCost = "{4}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Angel"
    power = 4
    toughness = 4
    oracleText = "Flying\nAt the beginning of each combat, choose first strike, vigilance, or lifelink. " +
        "Creatures you control gain that ability until end of turn."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.anyPlayer.beginningOf(Step.BEGIN_COMBAT)
        effect = Effects.ChooseAction(listOf(
            EffectChoice(
                label = "First strike",
                effect = Patterns.Group.grantKeywordToAll(Keyword.FIRST_STRIKE, Filters.Group.creaturesYouControl),
            ),
            EffectChoice(
                label = "Vigilance",
                effect = Patterns.Group.grantKeywordToAll(Keyword.VIGILANCE, Filters.Group.creaturesYouControl),
            ),
            EffectChoice(
                label = "Lifelink",
                effect = Patterns.Group.grantKeywordToAll(Keyword.LIFELINK, Filters.Group.creaturesYouControl),
            ),
        ))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "3"
        artist = "David Rapoza"
        flavorText = "\"A keen mind wields an army as a deft hand wields a sword.\""
        imageUri = "https://cards.scryfall.io/normal/front/b/e/beb04702-5cb2-4590-b675-9409ba52a395.jpg?1783940146"
        ruling("2013-01-24", "You choose which ability creatures you control will gain when Angelic Skirmisher's ability resolves. This happens before attacking creatures are declared.")
        ruling("2013-01-24", "Only creatures you control when the ability resolves will gain the chosen ability. Creatures that come under your control later in the turn will not gain the ability.")
    }
}
