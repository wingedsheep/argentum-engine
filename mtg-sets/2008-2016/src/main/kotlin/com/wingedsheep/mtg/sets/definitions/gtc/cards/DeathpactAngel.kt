package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedActivatedAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Deathpact Angel
 * {3}{W}{B}{B}
 * Creature — Angel
 * 5/5
 * Flying
 * When this creature dies, create a 1/1 white and black Cleric creature token. It has
 * "{3}{W}{B}{B}, {T}, Sacrifice this token: Return a card named Deathpact Angel from your
 * graveyard to the battlefield."
 *
 * The Cleric's ability is part of the token's copiable values (CR 111.3, CR 707.2): a copy of the
 * token has it too. "A card named Deathpact Angel" isn't targeted — the card is chosen as the
 * ability resolves, from whatever Deathpact Angels are in the graveyard then.
 */
val DeathpactAngel = card("Deathpact Angel") {
    manaCost = "{3}{W}{B}{B}"
    colorIdentity = "WB"
    typeLine = "Creature — Angel"
    power = 5
    toughness = 5
    oracleText = "Flying\nWhen this creature dies, create a 1/1 white and black Cleric creature token. " +
        "It has \"{3}{W}{B}{B}, {T}, Sacrifice this token: Return a card named Deathpact Angel from " +
        "your graveyard to the battlefield.\""

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE, Color.BLACK),
            creatureTypes = setOf("Cleric"),
            activatedAbilities = listOf(
                grantedActivatedAbility {
                    cost = Costs.Composite(Costs.Mana("{3}{W}{B}{B}"), Costs.Tap, Costs.SacrificeSelf)
                    effect = Effects.Pipeline {
                        val angels = gather(
                            CardSource.FromZone(
                                zone = Zone.GRAVEYARD,
                                player = Player.You,
                                filter = GameObjectFilter(
                                    cardPredicates = listOf(CardPredicate.NameEquals("Deathpact Angel"))
                                ),
                            )
                        )
                        val returning = chooseExactly(
                            1,
                            from = angels,
                            prompt = "Return a card named Deathpact Angel from your graveyard to the battlefield",
                        )
                        move(returning, CardDestination.ToZone(Zone.BATTLEFIELD))
                    }
                }
            ),
            imageUri = "https://cards.scryfall.io/normal/front/9/a/9af74e26-69d7-4683-9a71-9b420c03d75f.jpg?1783940147",
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "153"
        artist = "Jason Chan"
        imageUri = "https://cards.scryfall.io/normal/front/8/1/81cfc3c5-6d69-443e-a506-76b94178979b.jpg?1783940110"
    }
}
