package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GainKeywordsOfCards
import com.wingedsheep.sdk.scripting.KeywordCardPool
import com.wingedsheep.sdk.scripting.KeywordFamily
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Eater of Virtue — Kamigawa: Neon Dynasty #245 (canonical printing)
 * {1} · Legendary Artifact — Equipment
 *
 * The dies trigger and the keyword static are a linked pair (CR 607): the trigger exiles the dead
 * creature card into this Equipment's linked-exile pile, and [GainKeywordsOfCards] over
 * [KeywordCardPool.CARDS_EXILED_WITH_SOURCE] reads only that pile — never every card in exile. The
 * keywords are read live, so they apply to whatever creature it equips next, and a card that leaves
 * exile stops counting. "Hexproof" and "protection" are families: per the ruling, the creature gains
 * any hexproof-from and protection-from abilities of those cards, qualities kept.
 */
val EaterOfVirtue = card("Eater of Virtue") {
    manaCost = "{1}"
    colorIdentity = ""
    typeLine = "Legendary Artifact — Equipment"
    oracleText = "Whenever equipped creature dies, exile it.\n" +
        "Equipped creature gets +2/+0.\n" +
        "As long as a card exiled with Eater of Virtue has flying, equipped creature has flying. " +
        "The same is true for first strike, double strike, deathtouch, haste, hexproof, " +
        "indestructible, lifelink, menace, protection, reach, trample, and vigilance.\n" +
        "Equip {1}"

    triggeredAbility {
        trigger = Triggers.attached.dies()
        effect = Effects.ExileLinkedToSource(EffectTarget.TriggeringEntity)
        description = "Whenever equipped creature dies, exile it."
    }

    staticAbility {
        ability = ModifyStats(2, 0)
    }

    staticAbility {
        ability = GainKeywordsOfCards(
            keywords = listOf(
                Keyword.FLYING,
                Keyword.FIRST_STRIKE,
                Keyword.DOUBLE_STRIKE,
                Keyword.DEATHTOUCH,
                Keyword.HASTE,
                Keyword.INDESTRUCTIBLE,
                Keyword.LIFELINK,
                Keyword.MENACE,
                Keyword.REACH,
                Keyword.TRAMPLE,
                Keyword.VIGILANCE
            ),
            families = setOf(KeywordFamily.HEXPROOF, KeywordFamily.PROTECTION),
            cards = KeywordCardPool.CARDS_EXILED_WITH_SOURCE,
            filter = GroupFilter.attachedCreature()
        )
    }

    equipAbility("{1}")

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "245"
        artist = "Tuan Duong Chu"
        imageUri = "https://cards.scryfall.io/normal/front/3/4/34d92077-d7b2-4996-848f-fd4917e2fb42.jpg?1783923827"
        ruling("2022-02-18", "The Equipped creature gains any hexproof and protection abilities of cards exiled with Eater of Virtue.")
        ruling("2022-02-18", "If Eater of Virtue leaves the battlefield and returns to the battlefield, it will be a new object with no association to any cards it exiled from any previous times it was on the battlefield. It will have to exile new cards to grant abilities to creatures it equips from that point forward.")
    }
}
