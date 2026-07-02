package com.wingedsheep.mtg.sets.definitions.fin.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect
import com.wingedsheep.sdk.scripting.effects.ReturnFace
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Dion, Bahamut's Dominant // Bahamut, Warden of Light — Final Fantasy #16
 * {3}{W} · Legendary Creature — Human Noble Knight 3/3
 * // Legendary Enchantment Creature — Saga Dragon 5/5
 *
 * Front — Dion, Bahamut's Dominant:
 *   Dragonfire Dive — During your turn, Dion and other Knights you control have flying.
 *   When Dion enters, create a 2/2 white Knight creature token.
 *   {4}{W}{W}, {T}: Exile Dion, then return it to the battlefield transformed under its
 *   owner's control. Activate only as a sorcery.
 *
 * Back — Bahamut, Warden of Light (eikon Saga):
 *   (As this Saga enters and after your draw step, add a lore counter.)
 *   I, II — Wings of Light — Put a +1/+1 counter on each other creature you control. Those
 *   creatures gain flying until end of turn.
 *   III — Gigaflare — Destroy target permanent. Exile Bahamut, then return it to the
 *   battlefield (front face up).
 *   Flying
 *
 * Dragonfire Dive is two [Conditions.IsYourTurn]-gated statics — one on Dion himself and one
 * on *other* Knights you control — because per the official ruling Dion keeps flying during
 * your turn even if he somehow isn't a Knight. Wings of Light iterates the other-creatures
 * group once (snapshot), giving each a counter and end-of-turn flying, so "those creatures"
 * is exactly the set that received counters. The Dominant transform loop follows Jill,
 * Shiva's Dominant.
 */
private val BahamutWardenOfLight = card("Bahamut, Warden of Light") {
    manaCost = ""
    colorIdentity = "W"
    typeLine = "Legendary Enchantment Creature — Saga Dragon"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter.)\n" +
        "I, II — Wings of Light — Put a +1/+1 counter on each other creature you control. " +
        "Those creatures gain flying until end of turn.\n" +
        "III — Gigaflare — Destroy target permanent. Exile Bahamut, then return it to the " +
        "battlefield (front face up).\n" +
        "Flying"
    power = 5
    toughness = 5

    keywords(Keyword.FLYING)

    // I, II — Wings of Light — Put a +1/+1 counter on each other creature you control.
    // Those creatures gain flying until end of turn.
    val wingsOfLight = Effects.ForEachInGroup(
        GroupFilter.OtherCreaturesYouControl,
        Effects.Composite(
            Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self),
            Effects.GrantKeyword(Keyword.FLYING, EffectTarget.Self),
        ),
    )
    sagaChapter(1) {
        effect = wingsOfLight
    }
    sagaChapter(2) {
        effect = wingsOfLight
    }

    // III — Gigaflare — Destroy target permanent. Exile Bahamut, then return it to the
    // battlefield (front face up).
    sagaChapter(3) {
        val t = target("permanent", TargetObject(filter = TargetFilter(GameObjectFilter.Permanent)))
        effect = Effects.Composite(
            Effects.Destroy(t),
            Effects.ExileAndReturnTransformed(EffectTarget.Self, ReturnFace.FRONT),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "16"
        artist = "Kevin Glint"
        imageUri = "https://cards.scryfall.io/normal/back/8/c/8c0f9306-2058-476d-a711-bd37a6e15e42.jpg?1782686587"

        ruling(
            "2025-06-06",
            "If the target permanent is an illegal target when Bahamut's third chapter ability " +
                "tries to resolve, it won't resolve and none of its effects will happen. You " +
                "won't exile Bahamut and return it to the battlefield front face up."
        )
    }
}

private val DionBahamutsDominantFront = card("Dion, Bahamut's Dominant") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Human Noble Knight"
    oracleText = "Dragonfire Dive — During your turn, Dion and other Knights you control have " +
        "flying.\n" +
        "When Dion enters, create a 2/2 white Knight creature token.\n" +
        "{4}{W}{W}, {T}: Exile Dion, then return it to the battlefield transformed under its " +
        "owner's control. Activate only as a sorcery."
    power = 3
    toughness = 3

    // Dragonfire Dive — During your turn, Dion has flying (even if he isn't a Knight)...
    staticAbility {
        condition = Conditions.IsYourTurn
        ability = GrantKeyword(Keyword.FLYING, Filters.Self)
    }
    // ...and other Knights you control have flying.
    staticAbility {
        condition = Conditions.IsYourTurn
        ability = GrantKeyword(
            Keyword.FLYING,
            GroupFilter(
                GameObjectFilter.Creature.withSubtype("Knight").youControl(),
                excludeSelf = true,
            ),
        )
    }

    // When Dion enters, create a 2/2 white Knight creature token.
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = CreateTokenEffect(
            power = 2,
            toughness = 2,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Knight"),
            imageUri = "https://cards.scryfall.io/normal/front/a/7/a7758a0b-9e85-4b4a-bf1c-ffcc6761dbad.jpg?1782725381",
        )
    }

    // {4}{W}{W}, {T}: Exile Dion, then return it transformed. Activate only as a sorcery.
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{4}{W}{W}"), Costs.Tap)
        timing = TimingRule.SorcerySpeed
        effect = Effects.ExileAndReturnTransformed()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "16"
        artist = "Kevin Glint"
        imageUri = "https://cards.scryfall.io/normal/front/8/c/8c0f9306-2058-476d-a711-bd37a6e15e42.jpg?1782686587"

        ruling(
            "2025-06-06",
            "Dion's first ability will still give it flying during your turn even if Dion " +
                "somehow isn't a Knight."
        )
    }
}

val DionBahamutsDominant: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = DionBahamutsDominantFront,
    backFace = BahamutWardenOfLight,
)
