package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostGating
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Capricious Hellraiser — Phyrexia: All Will Be One #125
 * {3}{R}{R}{R} · Creature — Phyrexian Dragon · 4/4
 *
 * This spell costs {3} less to cast if you have nine or more cards in your graveyard.
 * Flying
 * When this creature enters, exile three cards at random from your graveyard. Choose a
 * noncreature, nonland card from among them and copy it. You may cast the copy without paying
 * its mana cost.
 *
 * The reduction is a self-cast [ModifySpellCost] gated on the graveyard size. The ETB composes
 * gather → random pick of three → exile → mandatory choice among the noncreature, nonland ones
 * (none when all three are creatures/lands — no choice, no copy) → copy → optional free cast.
 * An uncast copy ceases to exist via the Rule 707.10a state-based action.
 */
val CapriciousHellraiser = card("Capricious Hellraiser") {
    manaCost = "{3}{R}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Phyrexian Dragon"
    power = 4
    toughness = 4
    oracleText = "This spell costs {3} less to cast if you have nine or more cards in your graveyard.\n" +
        "Flying\n" +
        "When this creature enters, exile three cards at random from your graveyard. Choose a " +
        "noncreature, nonland card from among them and copy it. You may cast the copy without " +
        "paying its mana cost."

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGeneric(3),
            gating = CostGating.OnlyIf(Conditions.CardsInGraveyardAtLeast(9)),
        )
    }

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        description = "When this creature enters, exile three cards at random from your graveyard. " +
            "Choose a noncreature, nonland card from among them and copy it. You may cast the copy " +
            "without paying its mana cost."
        effect = Effects.Pipeline {
            val graveyard = gather(CardSource.FromZone(Zone.GRAVEYARD, Player.You))
            val exiled = chooseRandom(3, from = graveyard)
            exile(exiled)
            val eligible = filter(exiled, GameObjectFilter.Noncreature and GameObjectFilter.Nonland)
            val chosen = chooseExactly(
                1,
                from = eligible,
                prompt = "Choose a noncreature, nonland card to copy",
            )
            ifNotEmpty(chosen) {
                val copy = copyCards(chosen)
                run(Effects.May(
                    Effects.CastFromCollectionWithoutPayingCost(copy),
                    descriptionOverride = "You may cast the copy without paying its mana cost.",
                ))
            }
        }
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "125"
        artist = "Durion"
        imageUri = "https://cards.scryfall.io/normal/front/2/3/23afca1a-62a8-497f-994f-ceb479d020ba.jpg?1783918033"

        ruling(
            "2023-02-04",
            "The three cards you exile are chosen at random from among all cards in your graveyard. " +
                "If all three cards you exile this way are creature and/or land cards, you won't be " +
                "able to choose one of them to copy and cast. Capricious, indeed."
        )
        ruling(
            "2023-02-04",
            "You cast the copy while the ability is resolving and still on the stack. You can't wait " +
                "to cast it later in the turn."
        )
        ruling(
            "2023-02-04",
            "If the spell you cast has {X} in its mana cost, you must choose 0 as the value of X when " +
                "casting it without paying its mana cost."
        )
        ruling(
            "2023-02-04",
            "If you don't want to cast the copy, you can choose not to; the copy ceases to exist the " +
                "next time state-based actions are checked."
        )
    }
}
