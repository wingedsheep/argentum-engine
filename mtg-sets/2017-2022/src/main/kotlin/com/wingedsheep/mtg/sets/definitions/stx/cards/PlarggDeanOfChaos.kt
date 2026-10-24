package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Plargg, Dean of Chaos // Augusta, Dean of Order — Strixhaven: School of Mages #155 (canonical
 * printing)
 *
 * A modal double-faced card (CR 712) whose faces are both creatures, so it is the
 * [CardDefinition.modalDoubleFacedPermanent] shape (Jennifer Walters // The Sensational She-Hulk):
 * cast either face from hand for that face's own cost, and the permanent is that face.
 *
 * Front — Plargg, Dean of Chaos ({1}{R}, Legendary Creature — Orc Shaman, 2/2):
 *  - "{T}, Discard a card: Draw a card." A tap + discard composite cost.
 *  - "{4}{R}, {T}: Reveal cards from the top of your library until you reveal a nonlegendary,
 *    nonland card with mana value 3 or less. You may cast that card without paying its mana cost.
 *    Put all revealed cards not cast this way on the bottom of your library in a random order."
 *    Jodah, the Unifier's walk-until-match shape, but the cards are *revealed* and stay in the
 *    library (Sunbird's Invocation casts from there). The cast happens during resolution, so its
 *    normal timing is ignored (ruling). Only after the cast is offered are the cards still in the
 *    library — everything revealed, including a found card that wasn't cast — put on the bottom
 *    in a random order.
 *
 * Back — Augusta, Dean of Order ({2}{W}, Legendary Creature — Human Cleric, 1/3):
 *  - "Other tapped creatures you control get +1/+0." / "Other untapped creatures you control get
 *    +0/+1." Two lord statics over tapped / untapped creatures, excluding Augusta herself whatever
 *    her own tapped state (ruling).
 *  - "Whenever you attack, untap each creature you control, then tap any number of creatures you
 *    control." [Triggers.you] `attacks()` fires once per attack declaration whether or not Augusta
 *    attacked (ruling); every creature you control untaps (attackers stay attacking — ruling), then
 *    you choose any number of them on the battlefield to tap.
 */
private val PlarggDeanOfChaosFront = card("Plargg, Dean of Chaos") {
    manaCost = "{1}{R}"
    colorIdentity = "RW"
    typeLine = "Legendary Creature — Orc Shaman"
    power = 2
    toughness = 2
    oracleText = "{T}, Discard a card: Draw a card.\n" +
        "{4}{R}, {T}: Reveal cards from the top of your library until you reveal a nonlegendary, " +
        "nonland card with mana value 3 or less. You may cast that card without paying its mana " +
        "cost. Put all revealed cards not cast this way on the bottom of your library in a random order."

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.DiscardCard)
        effect = Effects.DrawCards(1)
        description = "Draw a card."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{4}{R}"), Costs.Tap)
        effect = Effects.Pipeline {
            val (found, revealed) = gatherUntilMatch(
                GameObjectFilter.Nonland.nonlegendary().manaValueAtMost(3)
            )
            reveal(revealed)
            ifNotEmpty(found) {
                run(Effects.May(Effects.CastFromCollectionWithoutPayingCost(found)))
            }
            val notCast = filter(revealed, GameObjectFilter.Any.currentlyIn(Zone.LIBRARY))
            toLibraryBottom(notCast, order = CardOrder.Random)
        }
        description = "Reveal cards from the top of your library until you reveal a nonlegendary, " +
            "nonland card with mana value 3 or less. You may cast that card without paying its mana " +
            "cost. Put all revealed cards not cast this way on the bottom of your library in a random order."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "155"
        artist = "Bryan Sola"
        imageUri = "https://cards.scryfall.io/normal/front/5/b/5bd9b5cf-f018-48af-a081-995ce8ecc539.jpg?1783927333"
        ruling("2021-04-16", "Plargg, Dean of Chaos's second ability allows you to cast the spell during that ability's resolution, ignoring its normal timing restrictions.")
    }
}

private val AugustaDeanOfOrderBack = card("Augusta, Dean of Order") {
    manaCost = "{2}{W}"
    colorIdentity = "RW"
    typeLine = "Legendary Creature — Human Cleric"
    power = 1
    toughness = 3
    oracleText = "Other tapped creatures you control get +1/+0.\n" +
        "Other untapped creatures you control get +0/+1.\n" +
        "Whenever you attack, untap each creature you control, then tap any number of creatures you control."

    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 0,
            filter = GroupFilter(GameObjectFilter.Creature.tapped().youControl(), excludeSelf = true)
        )
    }

    staticAbility {
        ability = ModifyStats(
            powerBonus = 0,
            toughnessBonus = 1,
            filter = GroupFilter(GameObjectFilter.Creature.untapped().youControl(), excludeSelf = true)
        )
    }

    triggeredAbility {
        trigger = Triggers.you.attacks()
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.youControl()),
            Effects.Untap(EffectTarget.IterationEntity)
        ) then Effects.Pipeline {
            val yourCreatures = gather(CardSource.ControlledPermanents(Player.You, GameObjectFilter.Creature))
            val toTap = chooseAnyNumber(
                from = yourCreatures,
                prompt = "Tap any number of creatures you control",
                selectedLabel = "Tap",
                useTargetingUI = true,
            )
            run(Effects.TapCollection(toTap, tap = true))
        }
        description = "Whenever you attack, untap each creature you control, then tap any number of " +
            "creatures you control."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "155"
        artist = "Bryan Sola"
        imageUri = "https://cards.scryfall.io/normal/back/5/b/5bd9b5cf-f018-48af-a081-995ce8ecc539.jpg?1783927333"
        ruling("2021-04-16", "Despite the term \"other tapped creatures,\" Augusta's first ability doesn't care whether Augusta is tapped or untapped. The same is true for the second ability. Both abilities apply as long as Augusta is on the battlefield.")
        ruling("2021-04-16", "Augusta's last ability triggers whenever you attack with any creatures, even if you didn't attack with Augusta.")
        ruling("2021-04-16", "Augusta's last ability untaps all creatures you control, not just the ones that attacked this turn. Then you choose which creatures to tap, if any.")
        ruling("2021-04-16", "Untapping an attacking creature doesn't remove it from combat. It's still attacking normally.")
    }
}

val PlarggDeanOfChaos: CardDefinition = CardDefinition.modalDoubleFacedPermanent(
    frontFace = PlarggDeanOfChaosFront,
    backFace = AugustaDeanOfOrderBack,
)
