package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Battle Mammoth — Kaldheim #160 (canonical printing; reprinted in CLB)
 * {3}{G}{G} Creature — Elephant 6/5
 * Trample
 * Whenever a permanent you control becomes the target of a spell or ability an opponent controls,
 * you may draw a card.
 * Foretell {2}{G}{G}
 *
 * The targeting trigger is Mila, Crafty Companion's: one trigger per permanent you control that an
 * opposing spell or ability targets (once per permanent even if targeted more than once), with the
 * draw optional on resolution.
 */
val BattleMammoth = card("Battle Mammoth") {
    manaCost = "{3}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elephant"
    power = 6
    toughness = 5
    oracleText = "Trample\n" +
        "Whenever a permanent you control becomes the target of a spell or ability an opponent controls, you may draw a card.\n" +
        "Foretell {2}{G}{G} (During your turn, you may pay {2} and exile this card from your hand face down. Cast it on a later turn for its foretell cost.)"

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Permanent.youControl()).becomesTarget(byOpponent = true)
        effect = Effects.May(Effects.DrawCards(1))
        description = "Whenever a permanent you control becomes the target of a spell or ability " +
            "an opponent controls, you may draw a card."
    }

    keywordAbility(KeywordAbility.foretell("{2}{G}{G}"))

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "160"
        artist = "Jesper Ejsing"
        imageUri = "https://cards.scryfall.io/normal/front/c/b/cb3377d9-eec3-49b9-a3a7-e540d2038f51.jpg?1783928221"
        ruling("2021-02-05", "Battle Mammoth's triggered ability resolves before the spell or ability that caused it to trigger. It will resolve even if that spell or ability is countered.")
        ruling("2021-02-05", "If a spell or ability an opponent controls targets a single permanent you control more than once, Battle Mammoth's triggered ability will trigger only once. However, if a spell or ability an opponent controls targets multiple permanents you control, Battle Mammoth's triggered ability will trigger once for each of those permanents.")
        ruling("2021-02-05", "You choose whether to draw a card as Battle Mammoth's triggered ability is resolving.")
    }
}
