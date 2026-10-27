package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * The Trickster-God's Heist — Kaldheim #232
 * {2}{U}{B} · Enchantment — Saga · Uncommon
 *
 * I — You may exchange control of two target creatures.
 * II — You may exchange control of two target nonbasic, noncreature permanents that share a card type.
 * III — Target player loses 3 life and you gain 3 life.
 *
 * Chapter II's "share a card type" is dependent target selection: the second slot's filter reads the
 * first through `sharingCardTypeWith(first)` (any card type — per the ruling, an artifact land and an
 * artifact pair up even though only one is a land), and a first choice with no possible partner is
 * not offered. Both targets are re-checked on resolution; if either is illegal the exchange doesn't
 * happen. "You may" is a resolution-time consent gate, so targets are still announced up front.
 */
val TheTricksterGodsHeist = card("The Trickster-God's Heist") {
    manaCost = "{2}{U}{B}"
    colorIdentity = "UB"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)\n" +
        "I — You may exchange control of two target creatures.\n" +
        "II — You may exchange control of two target nonbasic, noncreature permanents that share a card type.\n" +
        "III — Target player loses 3 life and you gain 3 life."

    sagaChapter(1) {
        val first = target(TargetFilter.Creature)
        val second = target(TargetFilter.Creature)
        effect = Effects.May(Effects.ExchangeControl(first, second))
    }

    sagaChapter(2) {
        val nonbasicNoncreature = GameObjectFilter.NoncreaturePermanent.nonbasic()
        val first = target(TargetFilter(nonbasicNoncreature))
        val second = target(TargetFilter(nonbasicNoncreature.sharingCardTypeWith(first)))
        effect = Effects.May(Effects.ExchangeControl(first, second))
    }

    sagaChapter(3) {
        val player = target(Targets.Player)
        effect = Effects.LoseLife(3, player) then Effects.GainLife(3)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "232"
        artist = "Randy Vargas"
        imageUri = "https://cards.scryfall.io/normal/front/4/a/4aa19a68-0d71-4123-a64d-8cb76f93cd74.jpg?1783928189"
        ruling("2021-02-05", "If one of the target permanents of the chapter I or chapter II ability is an illegal target as that ability resolves, the exchange won't happen. If both targets are illegal, the ability doesn't resolve.")
        ruling("2021-02-05", "For those two abilities, you don't have to control either target.")
        ruling("2021-02-05", "If the same player controls both target permanents as the chapter I or chapter II ability resolves, nothing happens.")
        ruling("2021-02-05", "Gaining control of a creature doesn't cause a player to gain control of any Auras or Equipment attached to it.")
        ruling("2021-02-05", "For the chapter II ability, either target can have card types the other does not. For example, you can target an artifact land and an artifact.")
    }
}
