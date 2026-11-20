package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Herald of Torment (Born of the Gods #75)
 *
 * A 3/3 flyer on its own body; bestowed, the Aura statics hand the host +3/+3 and flying. The upkeep
 * life loss is the Herald's own trigger, so it keeps firing for its controller whether it is a
 * creature or an Aura (even one enchanting an opponent's creature).
 */
val HeraldOfTorment = card("Herald of Torment") {
    manaCost = "{1}{B}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment Creature — Demon"
    power = 3
    toughness = 3
    oracleText = "Bestow {3}{B}{B} (If you cast this card for its bestow cost, it's an Aura spell with enchant creature. It becomes a creature again if it's not attached.)\n" +
        "Flying\n" +
        "At the beginning of your upkeep, you lose 1 life.\n" +
        "Enchanted creature gets +3/+3 and has flying."

    keywordAbility(KeywordAbility.bestow("{3}{B}{B}"))
    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.LoseLife(1, EffectTarget.Controller)
    }

    staticAbility { ability = ModifyStats(3, 3) }
    staticAbility { ability = GrantKeyword(Keyword.FLYING) }

    metadata {
        ruling("2014-02-01", "The triggered ability that causes you to lose life applies whether Herald of Torment is a creature or an Aura. The \"you\" in that ability refers to Herald of Torment's controller. If you control Herald of Torment as an Aura enchanting a creature another player controls, that ability will trigger at the beginning of your upkeep and cause you to lose 1 life.")
        ruling("2013-09-15", "On the stack, a spell with bestow is either a creature spell or an Aura spell. It's never both, although it's an enchantment spell in either case.")
        ruling("2013-09-15", "Unlike other Aura spells, an Aura spell with bestow isn't countered if its target is illegal as it begins to resolve. Rather, the effect making it an Aura spell ends, it loses enchant creature, it returns to being an enchantment creature spell, and it resolves and enters the battlefield as an enchantment creature.")
        ruling("2013-09-15", "Unlike other Auras, an Aura with bestow isn't put into its owner's graveyard if it becomes unattached. Rather, the effect making it an Aura ends, it loses enchant creature, and it remains on the battlefield as an enchantment creature. It can attack (and its {T} abilities can be activated, if it has any) on the turn it becomes unattached if it's been under your control continuously, even as an Aura, since your most recent turn began.")
        ruling("2013-09-15", "If a permanent with bestow enters the battlefield by any method other than being cast, it will be an enchantment creature. You can't choose to pay the bestow cost and have it become an Aura.")
        rarity = Rarity.RARE
        collectorNumber = "75"
        artist = "Vance Kovacs"
        imageUri = "https://cards.scryfall.io/normal/front/6/b/6bb7e5ab-57d7-4a86-b7c2-262932ac3344.jpg?1783939556"
    }
}
