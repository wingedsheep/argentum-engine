package com.wingedsheep.mtg.sets.definitions.m19.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.EmitLibrarySearchedEventEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Sarkhan, Dragonsoul {4}{R}{R}
 * Legendary Planeswalker — Sarkhan
 * Starting Loyalty: 5
 *
 * +2: Sarkhan deals 1 damage to each opponent and each creature your opponents control.
 * −3: Sarkhan deals 4 damage to target player or planeswalker.
 * −9: Search your library for any number of Dragon creature cards, put them onto the battlefield,
 *     then shuffle.
 *
 * The −9 search is unbounded, so it uses Sarkhan Unbroken's gather → chooseAnyNumber → move
 * pipeline rather than `Patterns.Library.searchLibrary` (which only offers "up to N").
 */
val SarkhanDragonsoul = card("Sarkhan, Dragonsoul") {
    manaCost = "{4}{R}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Planeswalker — Sarkhan"
    startingLoyalty = 5
    oracleText = "+2: Sarkhan deals 1 damage to each opponent and each creature your opponents control.\n" +
        "−3: Sarkhan deals 4 damage to target player or planeswalker.\n" +
        "−9: Search your library for any number of Dragon creature cards, put them onto the " +
        "battlefield, then shuffle."

    loyaltyAbility(+2) {
        effect = Effects.DealDamage(1, EffectTarget.PlayerRef(Player.EachOpponent)) then
            Patterns.Group.dealDamageToAll(1, GroupFilter(GameObjectFilter.Creature.opponentControls()))
    }

    loyaltyAbility(-3) {
        val victim = target(Targets.PlayerOrPlaneswalker)
        effect = Effects.DealDamage(4, victim)
    }

    loyaltyAbility(-9) {
        effect = Effects.Pipeline {
            val dragons = gather(
                CardSource.FromZone(
                    Zone.LIBRARY,
                    Player.You,
                    GameObjectFilter.Creature.withSubtype(Subtype.DRAGON)
                ),
                search = true
            )
            val found = chooseAnyNumber(
                from = dragons,
                prompt = "Search your library for any number of Dragon creature cards",
            )
            move(found, CardDestination.ToZone(Zone.BATTLEFIELD, player = Player.You))
            run(Effects.ShuffleLibrary())
            run(EmitLibrarySearchedEventEffect)
        }
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "296"
        artist = "Yongjae Choi"
        imageUri = "https://cards.scryfall.io/normal/front/5/9/59715c84-8eed-41ad-aaf9-cfcce4445254.jpg?1783934484"
        ruling("2018-07-13", "In a Two-Headed Giant game, Sarkhan's first ability causes the opposing team to lose 2 life.")
    }
}
