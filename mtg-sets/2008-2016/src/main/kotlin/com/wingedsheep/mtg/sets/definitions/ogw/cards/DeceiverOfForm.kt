package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Deceiver of Form
 * {6}{C}
 * Creature — Eldrazi
 * 8/8
 * At the beginning of combat on your turn, reveal the top card of your library. If a creature card
 * is revealed this way, you may have creatures you control other than this creature become copies
 * of that card until end of turn. You may put that card on the bottom of your library.
 *
 * The Vesuvan Drifter shape widened to a group: the revealed card stays in the library as the copy
 * source ([Effects.EachPermanentBecomesCopyOfTarget] with `sourceFromAnyZone = true`), and the
 * affected set is [GroupFilter.OtherCreaturesYouControl] rather than `Self`. The copy is a resolution
 * "may" gated on the revealed card being a creature card; the bottom-of-library choice is a separate
 * "may" offered whatever was revealed and whatever was chosen (ruling 2016-01-22).
 */
val DeceiverOfForm = card("Deceiver of Form") {
    manaCost = "{6}{C}"
    colorIdentity = ""
    typeLine = "Creature — Eldrazi"
    power = 8
    toughness = 8
    oracleText = "({C} represents colorless mana.)\n" +
        "At the beginning of combat on your turn, reveal the top card of your library. If a creature " +
        "card is revealed this way, you may have creatures you control other than this creature " +
        "become copies of that card until end of turn. You may put that card on the bottom of your library."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.BEGIN_COMBAT)
        effect = Effects.Pipeline {
            val revealed = gather(CardSource.TopOfLibrary(1, Player.You))
            reveal(revealed)

            // If a creature card is revealed this way, you may have the others become copies of it.
            val creature = filter(revealed, GameObjectFilter.Creature)
            run(
                Effects.If(
                    condition = whenMatches(creature, GameObjectFilter.Creature),
                    then = Effects.May(
                        Effects.EachPermanentBecomesCopyOfTarget(
                            target = creature.asTarget,
                            filter = GroupFilter.OtherCreaturesYouControl,
                            duration = Duration.EndOfTurn,
                            sourceFromAnyZone = true,
                        ),
                        prompt = "Have creatures you control other than Deceiver of Form become " +
                            "copies of the revealed card until end of turn?",
                    ),
                )
            )

            // You may put that card on the bottom of your library.
            val toBottom = chooseUpTo(
                1,
                from = revealed,
                prompt = "You may put the revealed card on the bottom of your library",
                selectedLabel = "Put on bottom",
                remainderLabel = "Leave on top",
            )
            toLibraryBottom(toBottom, order = CardOrder.Preserve)
        }
        description = "At the beginning of combat on your turn, reveal the top card of your library. " +
            "If a creature card is revealed this way, you may have creatures you control other than " +
            "this creature become copies of that card until end of turn. You may put that card on the " +
            "bottom of your library."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "1"
        artist = "Viktor Titov"
        imageUri = "https://cards.scryfall.io/normal/front/a/4/a416d54b-ac84-48ac-92f8-eeb348f26016.jpg?1783937931"

        ruling("2016-01-22", "You may put the card on the bottom of your library in any of the possible scenarios that could occur with this card. It doesn't matter whether the card is a creature card or not. If it is a creature card, it doesn't matter whether you chose to have the copy effect apply.")
        ruling("2016-01-22", "If applicable, creatures you control copy the printed values of the revealed creature card. They will retain any counters they had on them. Any Auras and/or Equipment that were attached with them will remain so.")
        ruling("2016-01-22", "Any noncopy effects that affected a creature you control, including ones that modified its power and/or toughness, will continue to apply.")
        ruling("2016-01-22", "If a creature you control becomes a copy of the revealed creature card, it's neither entering nor leaving the battlefield. Any enters-the-battlefield or leaves-the-battlefield abilities won't trigger.")
    }
}
