package com.wingedsheep.mtg.sets.definitions.mat.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.LookAtTopOfLibrary
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CopyExceptions
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Vesuvan Drifter
 * {2}{U}
 * Creature — Shapeshifter
 * 2/4
 *
 * Flying
 * You may look at the top card of your library any time.
 * At the beginning of each combat, you may reveal the top card of your library. If you reveal a
 * creature card this way, this creature becomes a copy of that card until end of turn, except it
 * has flying.
 *
 * The copy is [Effects.EachPermanentBecomesCopyOfTarget] with `affected = Self` and
 * `sourceFromAnyZone = true` (the copy source stays in the library), lasting until end of turn,
 * with flying added as a copiable exception (CR 707.9b).
 */
val VesuvanDrifter = card("Vesuvan Drifter") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Shapeshifter"
    power = 2
    toughness = 4
    oracleText = "Flying\nYou may look at the top card of your library any time.\n" +
        "At the beginning of each combat, you may reveal the top card of your library. If you " +
        "reveal a creature card this way, this creature becomes a copy of that card until end of " +
        "turn, except it has flying."

    keywords(Keyword.FLYING)

    staticAbility {
        ability = LookAtTopOfLibrary()
    }

    triggeredAbility {
        trigger = Triggers.anyPlayer.beginningOf(Step.BEGIN_COMBAT)
        effect = Effects.May(
            Effects.Pipeline {
                val revealed = gather(CardSource.TopOfLibrary(1, Player.You))
                reveal(revealed)
                val creature = filter(revealed, GameObjectFilter.Creature)
                run(
                    Effects.If(
                        condition = whenMatches(creature, GameObjectFilter.Creature),
                        then = Effects.EachPermanentBecomesCopyOfTarget(
                            target = creature.asTarget,
                            affected = EffectTarget.Self,
                            duration = Duration.EndOfTurn,
                            sourceFromAnyZone = true,
                            exceptions = CopyExceptions(addedKeywords = setOf(Keyword.FLYING)),
                        ),
                    )
                )
            },
            prompt = "Reveal the top card of your library?",
        )
        description = "At the beginning of each combat, you may reveal the top card of your " +
            "library. If you reveal a creature card this way, this creature becomes a copy of " +
            "that card until end of turn, except it has flying."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "10"
        artist = "Julia Metzger"
        imageUri = "https://cards.scryfall.io/normal/front/f/2/f2785902-2757-406b-bc75-6f05e0edc98d.jpg?1783916526"

        ruling("2023-05-12", "Vesuvan Drifter lets you look at the top card of your library whenever you want (with one restriction—see below), even if you don't have priority. This action doesn't use the stack. Knowing what that card is becomes part of the information you have access to, just like you can look at the cards in your hand.")
        ruling("2023-05-12", "If the top card of your library changes while you're casting a spell, playing a land, or activating an ability, you can't look at the new top card until you finish doing so. This means that if you cast the top card of your library, you can't look at the next one until you're done paying for that spell.")
        ruling("2023-05-12", "Notably, while Vesuvan Drifter is a copy of another card, it doesn't have its own abilities. You won't be able to look at the top card of your library unless another effect allows you to do so.")
        ruling("2023-05-12", "Vesuvan Drifter copies exactly what was printed on the original creature card and nothing else, except it has flying.")
        ruling("2023-05-12", "If the copied creature card has {X} in its mana cost, X is 0.")
        ruling("2023-05-12", "Any non-copy effect that began to apply to Vesuvan Drifter before it became a copy of a creature card will continue to apply to it after it becomes a copy. Auras and Equipment attached to Vesuvan Drifter will continue to work, as well as counters that change its power and toughness.")
    }
}
