package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeywordToOwnSpells
import com.wingedsheep.sdk.scripting.effects.CardSource

/**
 * Party Thrasher
 * {1}{R}
 * Creature — Lizard Wizard
 * 1/4
 * Noncreature spells you cast from exile have convoke.
 * At the beginning of your first main phase, you may discard a card. If you do, exile the top
 * two cards of your library, then choose one of them. You may play that card this turn.
 *
 * The static is Hoarding Broodlord's [GrantKeywordToOwnSpells] scoped to exile casts and
 * narrowed to noncreature spells. The trigger is Immersturm Raider's optional rummage
 * ([Effects.May] around [Effects.IfYouDo]) with Fireglass Mentor's exile-two-choose-one
 * pipeline as the payoff.
 */
val PartyThrasher = card("Party Thrasher") {
    manaCost = "{1}{R}"
    typeLine = "Creature — Lizard Wizard"
    power = 1
    toughness = 4
    oracleText = "Noncreature spells you cast from exile have convoke. (Each creature you tap while " +
        "casting a noncreature spell from exile pays for {1} or one mana of that creature's color.)\n" +
        "At the beginning of your first main phase, you may discard a card. If you do, exile the top " +
        "two cards of your library, then choose one of them. You may play that card this turn."

    staticAbility {
        ability = GrantKeywordToOwnSpells(
            keyword = Keyword.CONVOKE,
            spellFilter = GameObjectFilter.Noncreature,
            fromZone = Zone.EXILE
        )
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.PRECOMBAT_MAIN)
        effect = Effects.May(
            Effects.IfYouDo(
                Patterns.Hand.discardCards(1),
                Effects.Pipeline {
                    val exiled = gather(CardSource.TopOfLibrary(2))
                    exile(exiled)
                    val chosen = chooseExactly(1, from = exiled, prompt = "Choose a card you may play this turn")
                    run(Effects.GrantMayPlayFromExile(from = chosen))
                }
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "129"
        artist = "Leanna Crossan"
        imageUri = "https://cards.scryfall.io/normal/front/b/7/b7f8bb8d-c46a-4531-9525-6981a222b468.jpg?1783911269"
    }
}
