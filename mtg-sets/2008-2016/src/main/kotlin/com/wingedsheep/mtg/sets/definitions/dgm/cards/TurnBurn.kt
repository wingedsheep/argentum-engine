package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardLayout
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

val TurnBurn = card("Turn // Burn") {
    layout = CardLayout.SPLIT
    colorIdentity = "UR"
    keywords(Keyword.FUSE)

    face("Turn") {
        manaCost = "{2}{U}"
        typeLine = "Instant"
        oracleText = "Until end of turn, target creature loses all abilities and becomes a red Weird with base power and toughness 0/1.\nFuse (You may cast one or both halves of this card from your hand.)"
        spell {
            val creature = target(TargetFilter.Creature)
            effect = Effects.RemoveAllAbilities(creature, Duration.EndOfTurn) then
                Effects.SetCreatureSubtypes(setOf("Weird"), creature, Duration.EndOfTurn) then
                Effects.ChangeColor(creature, setOf(Color.RED), Duration.EndOfTurn) then
                Effects.SetBasePowerAndToughness(0, 1, creature, Duration.EndOfTurn)
        }
    }

    face("Burn") {
        manaCost = "{1}{R}"
        typeLine = "Instant"
        oracleText = "Burn deals 2 damage to any target.\nFuse (You may cast one or both halves of this card from your hand.)"
        spell {
            val any = target(Targets.Any)
            effect = Effects.DealDamage(2, any)
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "134"
        artist = "Ryan Barger"
        imageUri = "https://cards.scryfall.io/normal/front/8/d/8d7fdd59-6d76-4a0c-ac75-816345ef4a39.jpg?1783940014"
        ruling("2013-04-15", "Turn will cause the creature lose all other colors and creature types, but it will retain any other card types (such as artifact) it may have.")
        ruling("2013-04-15", "Turn overwrites all previous effects that set the targeted creature's power or toughness to specific values. Effects that set its power or toughness to specific values that start to apply after Turn resolves will overwrite this effect.")
        ruling("2013-04-15", "Turn doesn't counter abilities that have already triggered or been activated. In particular, there is no way to cast this to stop a creature's \"At the beginning of your upkeep\" or \"When this creature enters the battlefield\" abilities from triggering.")
        ruling("2013-04-15", "If the affected creature gains an ability after Turn resolves, it will keep that ability.")
        ruling("2013-04-15", "Effects that modify the targeted creature's power or toughness, such as the effects of Phytoburst or Legion's Initiative, will apply to it no matter when they started to take effect. The same is true for counters that change the creature's power or toughness (such as +1/+1 counters) and effects that switch its power and toughness.")
        ruling("2013-04-15", "If you're casting a split card with fuse from any zone other than your hand, you can't cast both halves. You'll only be able to cast one half or the other.")
        ruling("2013-04-15", "To cast a fused split spell, pay both of its mana costs. While the spell is on the stack, its mana value is the total amount of mana in both costs.")
        ruling("2013-04-15", "You can choose the same object as the target of each half of a fused split spell, if appropriate.")
        ruling("2013-04-15", "When resolving a fused split spell with multiple targets, treat it as you would any spell with multiple targets. If all targets are illegal when the spell tries to resolve, the spell doesn't resolve and none of its effects happen. If at least one target is still legal at that time, the spell resolves, but an illegal target can't perform any actions or have any actions performed on it.")
        ruling("2013-04-15", "When a fused split spell resolves, follow the instructions of the left half first, then the instructions on the right half.")
        ruling("2013-04-15", "On the stack, a split spell that hasn't been fused has only that half's characteristics and mana value. The other half is treated as though it didn't exist.")
        ruling("2013-04-15", "Some split cards with fuse have two monocolor halves of different colors. If such a card is cast as a fused split spell, the resulting spell is multicolored. If only one half is cast, the spell is the color of that half. While not on the stack, such a card is multicolored.")
        ruling("2013-04-15", "Some split cards with fuse have two halves that are both multicolored. That card is multicolored no matter which half is cast, or if both halves are cast. It's also multicolored while not on the stack.")
        ruling("2013-04-15", "If a player names a card, the player may name either half of a split card, but not both. A split card has the chosen name if one of its two names matches the chosen name.")
        ruling("2013-04-15", "If you cast a split card with fuse from your hand without paying its mana cost, you can choose to use its fuse ability and cast both halves without paying their mana costs.")
    }
}
