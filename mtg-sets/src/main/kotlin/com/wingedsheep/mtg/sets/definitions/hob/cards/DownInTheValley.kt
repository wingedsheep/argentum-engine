package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.effects.GrantTriggeredAbilityEffect
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/** "Elves you control" — the group chapters III and IV pump. */
private val elvesYouControl = GroupFilter(GameObjectFilter.Creature.withSubtype(Subtype.ELF).youControl())

/**
 * Down in the Valley
 * {2}{G}
 * Enchantment — Saga
 * (As this Saga enters and after your draw step, add a lore counter. Sacrifice after IV.)
 * I — Search your library for a basic land card, reveal it, put it into your hand, then shuffle.
 * II — This Saga gains "Landfall — Whenever a land you control enters, create a 1/1 green Elf
 * creature token."
 * III, IV — Elves you control get +1/+0 and gain vigilance until end of turn.
 *
 *  - **Chapter II grants the ability to the Saga itself**, not to a group: the wording is "*This
 *    Saga* gains …", so it is a [GrantTriggeredAbilityEffect] over [EffectTarget.Self] with
 *    [Duration.Permanent]. The grant outlives chapter III but not the Saga — sacrificing after IV
 *    (CR 714.4) takes the granted landfall trigger with it, which is why the token payoff only
 *    covers the two turns between chapter II and the sacrifice.
 *  - **Chapters III and IV are the same ability**, declared once in [valleyRally] and wired to
 *    both, exactly like the four identical chapters of The Misty Mountains Cold.
 *  - The pump filter is "Elves you control", not "Elf creatures you control" — but the printed
 *    "get +1/+0" only means anything on a creature, and a noncreature Elf permanent can't gain
 *    vigilance meaningfully either, so [Creature][GameObjectFilter.Creature] is the honest scope.
 */
val DownInTheValley = card("Down in the Valley") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after IV.)\n" +
        "I — Search your library for a basic land card, reveal it, put it into your hand, then shuffle.\n" +
        "II — This Saga gains \"Landfall — Whenever a land you control enters, create a 1/1 green " +
        "Elf creature token.\"\n" +
        "III, IV — Elves you control get +1/+0 and gain vigilance until end of turn."

    sagaChapter(1) {
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.BasicLand,
            destination = SearchDestination.HAND,
            reveal = true
        )
    }

    sagaChapter(2) {
        effect = GrantTriggeredAbilityEffect(
            ability = TriggeredAbility.create(
                trigger = Triggers.LandYouControlEnters.event,
                binding = Triggers.LandYouControlEnters.binding,
                effect = Effects.CreateToken(
                    power = 1,
                    toughness = 1,
                    colors = setOf(Color.GREEN),
                    creatureTypes = setOf("Elf"),
                    imageUri = "https://cards.scryfall.io/normal/front/7/6/761c7c31-c6c5-44e2-a845-f590542b6eda.jpg?1785497812",
                ),
                descriptionOverride = "Landfall — Whenever a land you control enters, create a 1/1 " +
                    "green Elf creature token."
            ),
            target = EffectTarget.Self,
            duration = Duration.Permanent
        )
    }

    sagaChapter(3) { effect = valleyRally() }
    sagaChapter(4) { effect = valleyRally() }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "124"
        artist = "Rovina Cai"
        imageUri = "https://cards.scryfall.io/normal/front/c/8/c8aa5179-475b-4cc8-b21e-205b475eb4cf.jpg?1784895042"
    }
}

/** The one chapter ability shared by III and IV. */
private fun valleyRally() =
    Patterns.Group.modifyStatsForAll(1, 0, elvesYouControl) then
        Patterns.Group.grantKeywordToAll(Keyword.VIGILANCE, elvesYouControl)
