package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Treacherous Blessing
 * {2}{B}
 * Enchantment
 * When this enchantment enters, draw three cards.
 * Whenever you cast a spell, you lose 1 life.
 * When this enchantment becomes the target of a spell or ability, sacrifice it.
 *
 * Skulking Ghost's becomes-the-target self-sacrifice plus a plain cast trigger. A spell targeting it
 * raises both triggers at once, so its controller orders them; the life is lost either way (ruling).
 */
val TreacherousBlessing = card("Treacherous Blessing") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment"
    oracleText = "When this enchantment enters, draw three cards.\n" +
        "Whenever you cast a spell, you lose 1 life.\n" +
        "When this enchantment becomes the target of a spell or ability, sacrifice it."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.DrawCards(3)
    }

    triggeredAbility {
        trigger = Triggers.you.casts()
        effect = Effects.LoseLife(1, EffectTarget.Controller)
    }

    triggeredAbility {
        trigger = Triggers.self.becomesTarget()
        effect = Effects.SacrificeTarget(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "117"
        artist = "Marco Teixeira"
        flavorText = "Phenax is generous with gifts and unforgiving of debts."
        imageUri = "https://cards.scryfall.io/normal/front/3/5/35b3ad6b-9966-489f-a4c9-2639a634fd62.jpg?1783931560"
        ruling(
            "2020-01-24",
            "An ability that triggers when a player casts a spell resolves before the spell that caused it " +
                "to trigger. It resolves even if that spell is countered."
        )
        ruling(
            "2020-01-24",
            "If you cast a spell that targets Treacherous Blessing, you choose whether you lose 1 life before " +
                "or after you sacrifice it. You lose 1 life even if you sacrifice it first."
        )
    }
}
