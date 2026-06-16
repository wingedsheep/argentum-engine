package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect
import com.wingedsheep.sdk.scripting.effects.WardCost
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Gisa, the Hellraiser
 * {3}{B}{B}
 * Legendary Creature — Human Warlock
 * 4/4
 *
 * Ward—{2}, Pay 2 life.
 * Skeletons and Zombies you control get +1/+1 and have menace.
 * Whenever you commit a crime, create two tapped 2/2 blue and black Zombie Rogue creature tokens.
 * This ability triggers only once each turn. (Targeting opponents, anything they control, and/or
 * cards in their graveyards is a crime.)
 *
 * - Ward—{2}, Pay 2 life is a single composite ward cost ([WardCost.Composite] of a mana part and a
 *   life part). When an opponent targets Gisa, they must pay both components or their spell/ability
 *   is countered (CR 702.21a).
 * - The lord is two static abilities over `Skeletons and Zombies you control`
 *   ([GameObjectFilter.Creature].withAnyOfSubtypes(Skeleton, Zombie).youControl()): a +1/+1
 *   [ModifyStats] (layer 7c) and a [GrantKeyword] of menace (layer 6). Gisa is a Human Warlock, so
 *   she doesn't buff herself; the filter carries no excludeSelf because the printed text says
 *   "Skeletons and Zombies you control", not "other".
 * - The crime payoff is the standard [Triggers.YouCommitCrime] trigger with `oncePerTurn = true`,
 *   creating two tapped 2/2 blue-black Zombie Rogue tokens (the canonical OTJ Zombie Rogue, cf.
 *   Outlaw Stitcher).
 */
val GisaTheHellraiser = card("Gisa, the Hellraiser") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Human Warlock"
    power = 4
    toughness = 4
    oracleText = "Ward—{2}, Pay 2 life.\n" +
        "Skeletons and Zombies you control get +1/+1 and have menace.\n" +
        "Whenever you commit a crime, create two tapped 2/2 blue and black Zombie Rogue creature " +
        "tokens. This ability triggers only once each turn. (Targeting opponents, anything they " +
        "control, and/or cards in their graveyards is a crime.)"

    keywordAbility(
        KeywordAbility.wardComposite(WardCost.Mana("{2}"), WardCost.Life(2))
    )

    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = GroupFilter(
                GameObjectFilter.Creature
                    .withAnyOfSubtypes(listOf(Subtype.SKELETON, Subtype.ZOMBIE))
                    .youControl()
            )
        )
    }

    staticAbility {
        ability = GrantKeyword(
            Keyword.MENACE,
            GroupFilter(
                GameObjectFilter.Creature
                    .withAnyOfSubtypes(listOf(Subtype.SKELETON, Subtype.ZOMBIE))
                    .youControl()
            )
        )
    }

    triggeredAbility {
        trigger = Triggers.YouCommitCrime
        oncePerTurn = true
        effect = CreateTokenEffect(
            count = DynamicAmount.Fixed(2),
            power = 2,
            toughness = 2,
            colors = setOf(Color.BLUE, Color.BLACK),
            creatureTypes = setOf("Zombie", "Rogue"),
            tapped = true,
            imageUri = "https://cards.scryfall.io/normal/front/7/4/74c7a0bd-6011-495a-b56c-8fa707dd7f12.jpg?1712316777"
        )
        description = "Whenever you commit a crime, create two tapped 2/2 blue and black Zombie " +
            "Rogue creature tokens. This ability triggers only once each turn."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "89"
        artist = "Chris Rahn"
        imageUri = "https://cards.scryfall.io/normal/front/d/b/db7c07b2-02b2-4e62-bf1b-4848e06eec28.jpg?1712355592"

        ruling("2024-04-12", "A player commits a crime as they cast a spell, activate an ability, or put a triggered ability on the stack that targets at least one opponent, at least one permanent, spell, or ability an opponent controls, and/or at least one card in an opponent's graveyard.")
        ruling("2024-04-12", "The spell or ability that constituted a crime doesn't have to have resolved yet or at all. As soon as you're finished casting the spell, activating the ability, or putting the triggered ability on the stack, you've committed a crime.")
        ruling("2024-04-12", "A player can commit only one crime per spell or ability they control. Targeting multiple opponents, permanents, spells, abilities, and/or cards with the same spell or ability doesn't constitute committing multiple crimes.")
    }
}
