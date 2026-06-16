package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.ReflexiveTriggerEffect
import com.wingedsheep.sdk.scripting.effects.SelectTargetEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Unscrupulous Contractor
 * {2}{B}
 * Creature — Human Assassin
 * 3/2
 *
 * When this creature enters, you may sacrifice a creature. When you do, target player
 * draws two cards and loses 2 life.
 * Plot {2}{B} (You may pay {2}{B} and exile this card from your hand. Cast it as a sorcery
 * on a later turn without paying its mana cost. Plot only as a sorcery.)
 *
 * "Sacrifice a creature" is a resolution-time choice, not a target (per the ruling: no target
 * is chosen when the ETB ability triggers). The player accepts the optional first, then chooses
 * which creature to sacrifice, so declining never forces a commitment. Only when a creature is
 * actually sacrificed does the reflexive "When you do" ability go on the stack, choosing its
 * target player as it does — modelled by [ReflexiveTriggerEffect] with a player
 * `reflexiveTargetRequirement`. The reflexive payoff is a composite: that player draws two
 * cards and loses 2 life. Plot is the standard [KeywordAbility.plot] special action.
 */
val UnscrupulousContractor = card("Unscrupulous Contractor") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Assassin"
    power = 3
    toughness = 2
    oracleText = "When this creature enters, you may sacrifice a creature. When you do, target " +
        "player draws two cards and loses 2 life.\n" +
        "Plot {2}{B} (You may pay {2}{B} and exile this card from your hand. Cast it as a sorcery " +
        "on a later turn without paying its mana cost. Plot only as a sorcery.)"

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = ReflexiveTriggerEffect(
            action = Effects.Composite(listOf(
                SelectTargetEffect(
                    requirement = TargetObject(filter = TargetFilter.CreatureYouControl),
                    storeAs = "creatureToSacrifice"
                ),
                Effects.SacrificeTarget(EffectTarget.PipelineTarget("creatureToSacrifice"))
            )),
            optional = true,
            reflexiveEffect = Effects.Composite(listOf(
                Effects.DrawCards(2, EffectTarget.ContextTarget(0)),
                Effects.LoseLife(2, EffectTarget.ContextTarget(0))
            )),
            reflexiveTargetRequirements = listOf(Targets.Player),
            descriptionOverride = "You may sacrifice a creature. When you do, target player draws two cards and loses 2 life."
        )
        description = "When this creature enters, you may sacrifice a creature. When you do, target player draws two cards and loses 2 life."
    }

    keywordAbility(KeywordAbility.plot("{2}{B}"))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "112"
        artist = "Mila Pesic"
        imageUri = "https://cards.scryfall.io/normal/front/9/e/9e56a8df-db04-4a88-a5ab-6954d3449976.jpg?1712355703"

        ruling("2024-04-12", "You don't choose a target for Unscrupulous Contractor's ability at the time it triggers. Rather, a second \"reflexive\" ability triggers if you sacrifice a creature this way. You choose a target for that ability as it goes on the stack.")
        ruling("2024-04-12", "You can't cast a plotted card on the same turn it became plotted. On any future turn, you may cast that card from exile without paying its mana cost during your main phase while the stack is empty.")
    }
}
