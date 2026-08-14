package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Dramatic Accusation — Murders at Karlov Manor #53
 * {2}{U} · Enchantment — Aura
 *
 * Enchant creature
 * When this Aura enters, tap enchanted creature.
 * Enchanted creature doesn't untap during its controller's untap step.
 * {U}{U}: Shuffle enchanted creature into its owner's library.
 *
 * A Claustrophobia that eventually becomes hard removal. The lock is the classic pair — an enters
 * trigger taps the creature once, and the static [AbilityFlag.DOESNT_UNTAP] keeps it that way — and
 * the {U}{U} ability upgrades the lock into exile-grade removal at instant speed once the mana is
 * spare.
 *
 * The tap is a *trigger*, not part of attaching, so it uses the stack and can be responded to; the
 * static, by contrast, applies continuously from the moment the Aura is attached. Neither targets
 * ("enchanted creature" is not a target, CR 702.155a), so hexproof on the enchanted creature stops
 * none of it once the Aura is on.
 *
 * The activated ability lives on the Aura, so only the Aura's controller may activate it (per the
 * ruling below) — enchanting an opponent's creature does not hand them the button. Shuffling the
 * creature away puts the Aura into its owner's graveyard as a state-based action, since it is no
 * longer attached to anything.
 */
val DramaticAccusation = card("Dramatic Accusation") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "When this Aura enters, tap enchanted creature.\n" +
        "Enchanted creature doesn't untap during its controller's untap step.\n" +
        "{U}{U}: Shuffle enchanted creature into its owner's library."

    auraTarget = Targets.Creature

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.Tap(EffectTarget.EnchantedCreature)
        description = "When this Aura enters, tap enchanted creature."
    }

    staticAbility {
        ability = GrantKeyword(AbilityFlag.DOESNT_UNTAP.name)
    }

    activatedAbility {
        cost = Costs.Mana("{U}{U}")
        effect = Effects.ShuffleIntoLibrary(EffectTarget.EnchantedCreature)
        description = "Shuffle enchanted creature into its owner's library"
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "53"
        artist = "Evyn Fong"
        imageUri = "https://cards.scryfall.io/normal/front/2/c/2ca93438-a132-45ca-9fa8-364aeb519594.jpg?1783912912"

        ruling("2024-02-02", "Only the controller of Dramatic Accusation may activate its ability.")
    }
}
