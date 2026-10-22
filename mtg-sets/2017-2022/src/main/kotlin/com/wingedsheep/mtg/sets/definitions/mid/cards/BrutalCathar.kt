package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.daybound
import com.wingedsheep.sdk.dsl.nightbound
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.WardCost
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Brutal Cathar // Moonrage Brute (Innistrad: Midnight Hunt)
 * {2}{W}
 * Creature — Human Soldier Werewolf // Creature — Werewolf
 *
 * Front (2/2): Whenever this creature enters or transforms into Brutal Cathar, exile target creature
 * an opponent controls until this creature leaves the battlefield. Daybound.
 * Back (3/3): First strike. Ward—Pay 3 life. Nightbound.
 *
 * "Enters or transforms into" is two triggers sharing one effect (Huntmaster of the Fells). The
 * linked exile (`LinkedExileComponent`) lives on the permanent and survives a transform — transforming
 * isn't leaving the battlefield — so both faces carry the leaves-the-battlefield return: the creature
 * may leave on either face, and the departed face's own abilities are the ones that look back.
 */
private const val EXILE_TEXT = "Whenever this creature enters or transforms into Brutal Cathar, exile target " +
    "creature an opponent controls until this creature leaves the battlefield."

private val BrutalCatharFront = card("Brutal Cathar") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier Werewolf"
    power = 2
    toughness = 2
    oracleText = "$EXILE_TEXT\n" +
        "Daybound (If a player casts no spells during their own turn, it becomes night next turn.)"

    triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.CreatureOpponentControls)
        effect = Effects.ExileUntilLeaves(creature)
        description = EXILE_TEXT
    }

    triggeredAbility {
        trigger = Triggers.self.transforms(intoBackFace = false)
        val creature = target(TargetFilter.CreatureOpponentControls)
        effect = Effects.ExileUntilLeaves(creature)
        description = EXILE_TEXT
    }

    triggeredAbility {
        trigger = Triggers.self.leaves()
        effect = Effects.ReturnLinkedExileUnderOwnersControl()
    }

    daybound()

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "7"
        artist = "Karl Kopinski"
        imageUri = "https://cards.scryfall.io/normal/front/0/d/0dbac7ce-a6fa-466e-b6ba-173cf2dec98e.jpg?1783925670"
    }
}

private val MoonrageBrute = card("Moonrage Brute") {
    manaCost = ""
    colorIdentity = "W"
    colorIndicator = "W" // Transformed back face, no mana cost (CR 204).
    typeLine = "Creature — Werewolf"
    power = 3
    toughness = 3
    oracleText = "First strike\n" +
        "Ward—Pay 3 life.\n" +
        "Nightbound (If a player casts at least two spells during their own turn, it becomes day next turn.)"

    keywords(Keyword.FIRST_STRIKE)
    keywordAbility(KeywordAbility.Ward(WardCost.Life(3)))

    // Brutal Cathar's exile lasts until the permanent leaves, whichever face is up when it does.
    triggeredAbility {
        trigger = Triggers.self.leaves()
        effect = Effects.ReturnLinkedExileUnderOwnersControl()
    }

    nightbound()

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "7"
        artist = "Karl Kopinski"
        imageUri = "https://cards.scryfall.io/normal/back/0/d/0dbac7ce-a6fa-466e-b6ba-173cf2dec98e.jpg?1783925670"
    }
}

val BrutalCathar: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = BrutalCatharFront,
    backFace = MoonrageBrute,
)
