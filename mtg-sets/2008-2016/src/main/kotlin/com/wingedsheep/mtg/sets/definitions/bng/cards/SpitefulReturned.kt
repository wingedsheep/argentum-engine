package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Spiteful Returned (Born of the Gods #84)
 *
 * "Whenever this creature or enchanted creature attacks" is written as two triggers, because
 * `Triggers.or` needs one binding and the two subjects bind differently (SELF vs ATTACHED). The
 * permanent is only ever one of the two at a time — a creature attacking on its own body, or a
 * bestowed Aura whose host attacks — so exactly one of them can fire per attack. "Defending
 * player" resolves through the attacking creature, i.e. the player it attacks.
 */
val SpitefulReturned = card("Spiteful Returned") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment Creature — Zombie"
    power = 1
    toughness = 1
    oracleText = "Bestow {3}{B} (If you cast this card for its bestow cost, it's an Aura spell with enchant creature. It becomes a creature again if it's not attached.)\n" +
        "Whenever this creature or enchanted creature attacks, defending player loses 2 life.\n" +
        "Enchanted creature gets +1/+1."

    keywordAbility(KeywordAbility.bestow("{3}{B}"))

    // "Whenever this creature ... attacks"
    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.LoseLife(2, EffectTarget.PlayerRef(Player.DefendingPlayer))
    }

    // "... or enchanted creature attacks"
    triggeredAbility {
        trigger = Triggers.attached.attacks()
        effect = Effects.LoseLife(2, EffectTarget.PlayerRef(Player.DefendingPlayer))
    }

    staticAbility { ability = ModifyStats(1, 1) }

    metadata {
        ruling("2013-09-15", "On the stack, a spell with bestow is either a creature spell or an Aura spell. It's never both, although it's an enchantment spell in either case.")
        ruling("2013-09-15", "Unlike other Aura spells, an Aura spell with bestow isn't countered if its target is illegal as it begins to resolve. Rather, the effect making it an Aura spell ends, it loses enchant creature, it returns to being an enchantment creature spell, and it resolves and enters the battlefield as an enchantment creature.")
        ruling("2013-09-15", "Unlike other Auras, an Aura with bestow isn't put into its owner's graveyard if it becomes unattached. Rather, the effect making it an Aura ends, it loses enchant creature, and it remains on the battlefield as an enchantment creature. It can attack (and its {T} abilities can be activated, if it has any) on the turn it becomes unattached if it's been under your control continuously, even as an Aura, since your most recent turn began.")
        ruling("2013-09-15", "If a permanent with bestow enters the battlefield by any method other than being cast, it will be an enchantment creature. You can't choose to pay the bestow cost and have it become an Aura.")
        ruling("2013-09-15", "Auras attached to a creature don't become tapped when the creature becomes tapped. Except in some rare cases, an Aura with bestow remains untapped when it becomes unattached and becomes a creature.")
        rarity = Rarity.UNCOMMON
        collectorNumber = "84"
        artist = "Raymond Swanland"
        imageUri = "https://cards.scryfall.io/normal/front/0/1/017e8cfb-7cb0-403c-b8be-abee22369661.jpg?1783939551"
    }
}
