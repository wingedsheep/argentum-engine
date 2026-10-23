package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Supertype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.effects.CopyExceptions
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Augmenter Pugilist // Echoing Equation — Strixhaven: School of Mages #147 (canonical printing)
 *
 * Modal double-faced card (CR 712). Cast either face from hand, never both.
 *
 * Front — Augmenter Pugilist ({1}{G}{G}, Creature — Troll Druid, 3/3):
 *   "Trample"
 *   "As long as you control eight or more lands, this creature gets +5/+5."
 *   Scurrid Colony's shape: a self-scoped [ModifyStats] gated by [Conditions.ControlLandsAtLeast].
 *
 * Back — Echoing Equation ({3}{U}{U}, Sorcery):
 *   "Choose target creature you control. Each other creature you control becomes a copy of it
 *    until end of turn, except those creatures aren't legendary."
 *   [Effects.EachPermanentBecomesCopyOfTarget] over creatures you control, `excludeTarget = true`
 *   (the Naga Fleshcrafter "each other" shape), `Duration.EndOfTurn`, with the "aren't legendary"
 *   exception as `CopyExceptions(removedSupertypes = {LEGENDARY})` (Shuri, Wakandan Inventor).
 *   Copiable values only (CR 707.2) — counters and other effects on the target aren't copied, and
 *   only creatures you control as it resolves are affected.
 */
val AugmenterPugilist = card("Augmenter Pugilist") {
    manaCost = "{1}{G}{G}"
    colorIdentity = "GU"
    typeLine = "Creature — Troll Druid"
    oracleText = "Trample\n" +
        "As long as you control eight or more lands, this creature gets +5/+5."
    power = 3
    toughness = 3

    keywords(Keyword.TRAMPLE)

    staticAbility {
        ability = ModifyStats(5, 5, Filters.Self)
        condition = Conditions.ControlLandsAtLeast(8)
    }

    modalBack("Echoing Equation") {
        manaCost = "{3}{U}{U}"
        typeLine = "Sorcery"
        oracleText = "Choose target creature you control. Each other creature you control becomes a " +
            "copy of it until end of turn, except those creatures aren't legendary."
        imageUri = "https://cards.scryfall.io/normal/back/d/9/d9131fc3-018a-4975-8795-47be3956160d.jpg?1783927337"
        spell {
            val creature = target(TargetFilter.CreatureYouControl)
            effect = Effects.EachPermanentBecomesCopyOfTarget(
                target = creature,
                filter = GroupFilter(GameObjectFilter.Creature.youControl()),
                duration = Duration.EndOfTurn,
                excludeTarget = true,
                exceptions = CopyExceptions(removedSupertypes = setOf(Supertype.LEGENDARY)),
            )
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "147"
        artist = "Volkan Baǵa"
        flavorText = "\"Hold still. I have a theory about what's going to happen to your face.\""
        imageUri = "https://cards.scryfall.io/normal/front/d/9/d9131fc3-018a-4975-8795-47be3956160d.jpg?1783927337"
        ruling("4/16/2021", "Because damage remains marked on creatures until the damage is removed as the turn ends, nonlethal damage dealt to Augmenter Pugilist may become lethal if the number of lands you control falls below eight.")
        ruling("4/16/2021", "Echoing Equation copies the printed values of the target creature. It won't copy any counters on that creature or any effects that have changed its power, toughness, types, color, and so on, and a creature becoming a copy doesn't remove any such counters or effects that are already applying to it.")
        ruling("4/16/2021", "If the chosen creature is copying something else, other creatures you control become copies of whatever the chosen creature is copying.")
        ruling("4/16/2021", "Echoing Equation affects only creatures you control at the time it resolves. Creatures that come under your control later in the turn won't be copies of the chosen creature.")
    }
}
