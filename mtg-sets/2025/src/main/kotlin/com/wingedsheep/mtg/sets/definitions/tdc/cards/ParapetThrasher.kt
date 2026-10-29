package com.wingedsheep.mtg.sets.definitions.tdc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Parapet Thrasher — Tarkir: Dragonstorm Commander #36 (canonical printing)
 * {2}{R}{R} · Creature — Dragon · 4/3
 *
 * Flying
 * Whenever one or more Dragons you control deal combat damage to an opponent, choose one that
 * hasn't been chosen this turn —
 * • Destroy target artifact that opponent controls.
 * • This creature deals 4 damage to each other opponent.
 * • Exile the top card of your library. You may play it this turn.
 *
 * One trigger per opponent hit (`dealCombatDamageToAnOpponent`), that opponent bound as
 * `Player.TriggeringPlayer`. "Each other opponent" is every opponent but that one: the
 * `ForEachPlayer` loop rebinds "you" to the visited opponent, so `Not(TriggeringPlayerIs(You))`
 * skips the damaged one. Mode memory is [ModalEffect.chooseOneNotYetChosenThisTurn]; a fourth
 * trigger in a turn has no mode left and does nothing (ruling).
 */
val ParapetThrasher = card("Parapet Thrasher") {
    manaCost = "{2}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Dragon"
    power = 4
    toughness = 3
    oracleText = "Flying\n" +
        "Whenever one or more Dragons you control deal combat damage to an opponent, choose one that hasn't been chosen this turn —\n" +
        "• Destroy target artifact that opponent controls.\n" +
        "• This creature deals 4 damage to each other opponent.\n" +
        "• Exile the top card of your library. You may play it this turn."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.oneOrMore(GameObjectFilter.Creature.withSubtype("Dragon")).dealCombatDamageToAnOpponent()
        effect = ModalEffect.chooseOneNotYetChosenThisTurn(
            mode("Destroy target artifact that opponent controls") {
                val artifact = target(TargetFilter(GameObjectFilter.Artifact.controlledByTriggeringPlayer()))
                effect = Effects.Destroy(artifact)
            },
            Mode.noTarget(
                Effects.ForEachPlayer(
                    Player.EachOpponent,
                    Effects.If(
                        Conditions.Not(Conditions.TriggeringPlayerIs(Player.You)),
                        Effects.DealDamage(4, EffectTarget.Controller)
                    )
                ),
                "This creature deals 4 damage to each other opponent",
            ),
            Mode.noTarget(
                Patterns.Exile.impulse(count = 1),
                "Exile the top card of your library. You may play it this turn",
            ),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "36"
        artist = "Cristi Balanescu"
        imageUri = "https://cards.scryfall.io/normal/front/6/d/6d3ba135-e8d6-443a-952b-5c93edb4523c.jpg?1783907173"
        ruling("2025-04-04", "If the triggered ability triggers more than three times in a turn, no mode can be chosen for any instances of the ability after the third, and those will be removed from the stack and do nothing.")
        ruling("2025-04-04", "You pay all costs and follow all timing rules for cards played this way. For example, if the exiled card is a land card, you may play it only during your main phase while the stack is empty.")
    }
}
