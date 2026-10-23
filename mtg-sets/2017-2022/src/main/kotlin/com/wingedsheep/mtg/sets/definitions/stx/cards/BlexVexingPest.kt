package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.times
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Blex, Vexing Pest // Search for Blex — Strixhaven: School of Mages #148 (canonical printing)
 *
 * Modal double-faced card (CR 712). Cast either face from hand, never both.
 *
 * Front — Blex, Vexing Pest ({2}{G}, Legendary Creature — Pest, 3/2):
 *   "Other Pests, Bats, Insects, Snakes, and Spiders you control get +1/+1."
 *   One lord static over permanents with any of the five subtypes (OR logic), so a creature that
 *   is several of those types gets the bonus only once, per the ruling.
 *   "When Blex dies, you gain 4 life."
 *
 * Back — Search for Blex ({2}{B}{B}, Sorcery):
 *   "Look at the top five cards of your library. You may put any number of them into your hand
 *    and the rest into your graveyard. You lose 3 life for each card you put into your hand this
 *    way."
 *   A look-at pipeline split into hand / graveyard, then life loss of 3 × the kept count. It is
 *   life *loss*, not a payment, so the choice isn't capped by your life total (ruling).
 */
val BlexVexingPest = card("Blex, Vexing Pest") {
    manaCost = "{2}{G}"
    colorIdentity = "BG"
    typeLine = "Legendary Creature — Pest"
    oracleText = "Other Pests, Bats, Insects, Snakes, and Spiders you control get +1/+1.\n" +
        "When Blex dies, you gain 4 life."
    power = 3
    toughness = 2

    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = GroupFilter(
                GameObjectFilter.Permanent.withAnyOfSubtypes(
                    listOf(Subtype("Pest"), Subtype.BAT, Subtype.INSECT, Subtype.SNAKE, Subtype.SPIDER)
                ).youControl(),
                excludeSelf = true
            )
        )
    }

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.GainLife(4)
    }

    modalBack("Search for Blex") {
        manaCost = "{2}{B}{B}"
        typeLine = "Sorcery"
        oracleText = "Look at the top five cards of your library. You may put any number of them " +
            "into your hand and the rest into your graveyard. You lose 3 life for each card you " +
            "put into your hand this way."
        imageUri = "https://cards.scryfall.io/normal/back/c/2/c204b7ca-0904-40fa-b20c-92400fae20b8.jpg?1783927336"
        spell {
            effect = Effects.Pipeline {
                val looked = gather(CardSource.TopOfLibrary(5))
                val (kept, rest) = chooseAnyNumberSplit(
                    from = looked,
                    prompt = "Put any number into your hand (lose 3 life each); the rest go into your graveyard",
                    selectedLabel = "Put into your hand (lose 3 life each)",
                    remainderLabel = "Put into your graveyard",
                )
                toHand(kept)
                toGraveyard(rest)
                run(Effects.LoseLife(kept.count * 3, EffectTarget.Controller))
            }
        }
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "148"
        artist = "Ekaterina Burmak"
        flavorText = "Pests are typically harvested before they outgrow their enclosures. Blex is far from typical."
        imageUri = "https://cards.scryfall.io/normal/front/c/2/c204b7ca-0904-40fa-b20c-92400fae20b8.jpg?1783927336"
        ruling("4/16/2021", "If a creature is more than one of the creature types Blex cares about, it may haunt your dreams, but it will get the +1/+1 bonus only once.")
        ruling("4/16/2021", "While resolving Search for Blex, you may put any number of the cards into your hand, even if you don't have enough life to cover it. We'll assume you have a plan that's better than \"and then I'll lose the game.\"")
    }
}
