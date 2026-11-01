package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Rakdos Firewheeler — Ravnica Allegiance #197
 * {B}{B}{R}{R} · Creature — Human Rogue · 4/3
 *
 * When this creature enters, it deals 2 damage to target opponent and 2 damage to up to one
 * target creature or planeswalker.
 *
 * The opponent target is mandatory (no opponent to target → the trigger doesn't go on the stack);
 * the creature-or-planeswalker target is optional. Each damage instance is dealt by this creature.
 */
val RakdosFirewheeler = card("Rakdos Firewheeler") {
    manaCost = "{B}{B}{R}{R}"
    colorIdentity = "BR"
    typeLine = "Creature — Human Rogue"
    power = 4
    toughness = 3
    oracleText = "When this creature enters, it deals 2 damage to target opponent and 2 damage to " +
        "up to one target creature or planeswalker."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val opponent = target(Targets.Opponent)
        val victim = target(Targets.CreatureOrPlaneswalker, optional = true)
        effect = Effects.DealDamage(2, opponent, damageSource = EffectTarget.Self) then
            Effects.DealDamage(2, victim, damageSource = EffectTarget.Self)
        description = "When this creature enters, it deals 2 damage to target opponent and 2 damage to " +
            "up to one target creature or planeswalker."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "197"
        artist = "Slawomir Maniak"
        flavorText = "Ever wonder why you never see an old Rakdos cultist?"
        imageUri = "https://cards.scryfall.io/normal/front/d/8/d819a944-0b3e-4e26-87da-2417081928e7.jpg?1783933640"
        ruling("2019-01-25", "If you can't target any opponent with Rakdos Firewheeler's ability, the ability doesn't go on the stack. You can't have it deal 2 damage to any creature or planeswalker. On the other hand, if you do choose two legal targets but either target is illegal when the ability tries to resolve, the other is still affected as appropriate.")
    }
}
