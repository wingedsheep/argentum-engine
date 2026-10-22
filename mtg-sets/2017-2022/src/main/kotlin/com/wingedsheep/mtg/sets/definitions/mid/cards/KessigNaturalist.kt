package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.daybound
import com.wingedsheep.sdk.dsl.nightbound
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.effects.EffectChoice
import com.wingedsheep.sdk.scripting.effects.ManaExpiry
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Kessig Naturalist // Lord of the Ulvenwald (Innistrad: Midnight Hunt)
 * {R}{G}
 * Creature — Human Werewolf // Creature — Werewolf
 *
 * Front (2/2): Whenever this creature attacks, add {R} or {G}. Until end of turn, you don't lose
 * this mana as steps and phases end. Daybound.
 * Back (3/3): Other Wolves and Werewolves you control get +1/+1. Same attack trigger. Nightbound.
 *
 * "{R} or {G}" is chosen as the trigger resolves (a [Effects.ChooseAction] between two kept-mana
 * adds), not as a mode when it goes on the stack. Only the added mana is tagged
 * [ManaExpiry.KEPT_UNTIL_END_OF_TURN]; the rest of the pool still empties normally.
 */
private const val ATTACK_MANA_TEXT = "Whenever this creature attacks, add {R} or {G}. " +
    "Until end of turn, you don't lose this mana as steps and phases end."

private fun keptRedOrGreen() = Effects.ChooseAction(
    listOf(
        EffectChoice(
            label = "Add {R}",
            effect = Effects.AddMana(Color.RED, 1, expiry = ManaExpiry.KEPT_UNTIL_END_OF_TURN),
        ),
        EffectChoice(
            label = "Add {G}",
            effect = Effects.AddMana(Color.GREEN, 1, expiry = ManaExpiry.KEPT_UNTIL_END_OF_TURN),
        ),
    )
)

private val KessigNaturalistFront = card("Kessig Naturalist") {
    manaCost = "{R}{G}"
    colorIdentity = "RG"
    typeLine = "Creature — Human Werewolf"
    power = 2
    toughness = 2
    oracleText = "$ATTACK_MANA_TEXT\n" +
        "Daybound (If a player casts no spells during their own turn, it becomes night next turn.)"

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = keptRedOrGreen()
        description = ATTACK_MANA_TEXT
    }

    daybound()

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "231"
        artist = "Johan Grenier"
        flavorText = "\"Far from home, little one? I know the feeling.\""
        imageUri = "https://cards.scryfall.io/normal/front/8/a/8ab5f2e6-0e0a-4f7d-a959-3d07948ff317.jpg?1783925563"
    }
}

private val LordOfTheUlvenwald = card("Lord of the Ulvenwald") {
    manaCost = ""
    colorIdentity = "RG"
    colorIndicator = "RG" // Transformed back face, no mana cost (CR 204).
    typeLine = "Creature — Werewolf"
    power = 3
    toughness = 3
    oracleText = "Other Wolves and Werewolves you control get +1/+1.\n" +
        "$ATTACK_MANA_TEXT\n" +
        "Nightbound (If a player casts at least two spells during their own turn, it becomes day next turn.)"

    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = GroupFilter(
                GameObjectFilter.Creature.withAnyOfSubtypes(listOf(Subtype.WOLF, Subtype.WEREWOLF)).youControl(),
                excludeSelf = true,
            ),
        )
    }

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = keptRedOrGreen()
        description = ATTACK_MANA_TEXT
    }

    nightbound()

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "231"
        artist = "Johan Grenier"
        imageUri = "https://cards.scryfall.io/normal/back/8/a/8ab5f2e6-0e0a-4f7d-a959-3d07948ff317.jpg?1783925563"
    }
}

val KessigNaturalist: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = KessigNaturalistFront,
    backFace = LordOfTheUlvenwald,
)
