package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Song-Mad Treachery {3}{R}{R} // Song-Mad Ruins — Zendikar Rising #165
 * Sorcery // Land · Modal double-faced card
 *
 * Front — Song-Mad Treachery: "Gain control of target creature until end of turn. Untap that
 * creature. It gains haste until end of turn."
 * Back — Song-Mad Ruins: "This land enters tapped. {T}: Add {R}."
 *
 * The front is the Threaten composition (gain control, untap, grant haste) in printed order.
 */
private val SongMadTreacheryFront = card("Song-Mad Treachery") {
    manaCost = "{3}{R}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Gain control of target creature until end of turn. Untap that creature. It gains haste until end of turn."

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.GainControl(t, Duration.EndOfTurn) then
            Effects.Untap(t) then
            Effects.GrantKeyword(Keyword.HASTE, t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "165"
        artist = "Zoltan Boros"
        flavorText = "\"I'm not proud of the things I did. But all I could think about at the time was how to make the noise stop.\"\n—Yavotz, Sea Gate Expeditionary House"
        imageUri = "https://cards.scryfall.io/normal/front/7/8/782ca27f-9f18-476c-b582-89c06fb2e322.jpg?1783929353"
    }
}

private val SongMadRuinsBack = card("Song-Mad Ruins") {
    typeLine = "Land"
    colorIdentity = "R"
    oracleText = "This land enters tapped.\n{T}: Add {R}."

    replacementEffect(EntersTapped())

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.RED)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "165"
        artist = "Zoltan Boros"
        flavorText = "\"They call it the Singing City, but those eerie sounds aren't music. They worm their way into your brain until rational thought is impossible.\"\n—Yavotz, Sea Gate Expeditionary House"
        imageUri = "https://cards.scryfall.io/normal/back/7/8/782ca27f-9f18-476c-b582-89c06fb2e322.jpg?1783929353"
    }
}

val SongMadTreachery: CardDefinition = CardDefinition.modalDoubleFacedLand(
    frontFace = SongMadTreacheryFront,
    backFace = SongMadRuinsBack,
)
